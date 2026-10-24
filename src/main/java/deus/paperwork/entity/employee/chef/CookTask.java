package deus.paperwork.entity.employee.chef;

import com.mojang.nbt.tags.CompoundTag;
import deus.brainless.task.StopReason;
import deus.brainless.task.Task;
import deus.brainless.task.TaskStatus;
import deus.paperwork.Paperwork;
import deus.paperwork.entity.base.MobWorldAccess;
import deus.paperwork.entity.employee.MobEmployee;
import deus.paperwork.core.util.PoscArea;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.entity.TileEntityChest;
import net.minecraft.core.block.entity.TileEntityFurnace;
import net.minecraft.core.data.registry.Registries;
import net.minecraft.core.data.registry.recipe.entry.RecipeEntryFurnace;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import net.minecraft.core.world.pos.TilePosc;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/// Cooks raw ingredients taken from a chest inside the chef's workplace and
/// deposits the cooked result into a different chest.
///
/// Before cooking, the chef makes sure the furnace has fuel: if it does not,
/// it looks for a chest with coal inside the workplace, takes one piece of
/// coal and puts it into the furnace fuel slot.
///
/// The task runs as an internal state machine:
/// <ul>
///   <li>{@link Phase#SCAN_SOURCE} — find a chest inside the workplace that holds items.</li>
///   <li>{@link Phase#MOVE_TO_SOURCE} — walk to that chest.</li>
///   <li>{@link Phase#TAKE_RAW} — take one raw item into the chef's inventory.</li>
///   <li>{@link Phase#SCAN_COOK_STATION} — find a furnace or workbench inside the workplace.</li>
///   <li>{@link Phase#MOVE_TO_COOK} — walk to the cook station.</li>
///   <li>{@link Phase#REFUEL_CHECK} — if the station is a furnace without fuel,
///       look for coal in a chest; otherwise proceed to cook.</li>
///   <li>{@link Phase#SCAN_FUEL_SOURCE} — find a chest with coal inside the workplace.</li>
///   <li>{@link Phase#MOVE_TO_FUEL_SOURCE} — walk to the coal chest.</li>
///   <li>{@link Phase#TAKE_FUEL} — take one piece of coal into the inventory.</li>
///   <li>{@link Phase#REFUEL_FURNACE} — put the coal into the furnace fuel slot.</li>
///   <li>{@link Phase#COOKING} — put one raw item into the furnace ingredient
///       slot (or prepare the recipe result directly on a workbench).</li>
///   <li>{@link Phase#WAIT_FOR_SMELT} — wait for the furnace to finish and take
///       the result out.</li>
///   <li>{@link Phase#SCAN_DEST} — find a chest inside the workplace that is NOT the source chest.</li>
///   <li>{@link Phase#WAITING_FOR_DEST} — no destination chest yet; keep the cooked food in the
///       inventory and re-scan every tick until one appears.</li>
///   <li>{@link Phase#MOVE_TO_DEST} — walk to the destination chest.</li>
///   <li>{@link Phase#DEPOSIT} — put the cooked food into the destination chest.</li>
/// </ul>
///
/// Succeeds once the cooked food has been deposited; fails when the workplace
/// is missing, too small/too large, has no source chest with items, or has no
/// cook station. Runs (RUNNING) while waiting for a destination chest.
public class CookTask implements Task {

	/// NBT key used to mark an item stack as "cooked" so the chef can tell
	/// cooked food apart from raw ingredients in its inventory.
	private static final String NBT_COOKED = "cooked";

	/// Slot index of the ingredient, fuel and result slots inside a furnace.
	private static final int FURNACE_SLOT_INGREDIENT = 0;
	private static final int FURNACE_SLOT_FUEL = 1;
	private static final int FURNACE_SLOT_RESULT = 2;

	/// How long the chef waits for a furnace to finish smelting before giving
	/// up on the current item (vanilla smelting takes 200 ticks).
	private static final int SMELT_TIMEOUT_TICKS = 400;

	/// How close to a chest or cook station counts as being there.
	private static final double ACTION_REACH = 1.5;

	/// Minimum / maximum side length of the workplace area.
	private static final int MIN_WIDTH = 3;
	private static final int MIN_LENGTH = 6;
	private static final int MAX_SIDE = 16;

	private final MobEmployee mob;

	private Phase phase = Phase.SCAN_SOURCE;
	private TilePosc currentTarget = null;
	private TilePosc sourceChest = null;
	private TilePosc destChest = null;
	/// Position of the furnace/cook station currently being used (remembered
	/// when the chef leaves to fetch coal).
	private TilePosc cookStationPos = null;
	/// Whether the current cook station is a furnace (needs fuel) or a
	/// workbench (does not need fuel).
	private boolean currentStationIsFurnace = false;
	/// Guards the size warning so an invalid area is reported once per task,
	/// not sixty times a second.
	private boolean sizeWarned = false;
	/// The chest the chef is currently taking coal from (kept apart from
	/// {@link #currentTarget}, which is cleared once the chef arrives).
	private TilePosc fuelChest = null;
	/// Ticks spent waiting for the current furnace smelt.
	private int smeltTicks = 0;

	public CookTask(MobEmployee mob) {
		this.mob = mob;
	}

	@Override
	public void start() {
		phase = Phase.SCAN_SOURCE;
		currentTarget = null;
		cookStationPos = null;
		currentStationIsFurnace = false;
		sizeWarned = false;
		fuelChest = null;
		smeltTicks = 0;
		// Restore the last source/destination chests from the job state so an
		// interrupted cook cycle can resume where it left off.
		if (mob.jobState() instanceof ChefJobState state) {
			sourceChest = state.sourceChest().orElse(null);
			destChest = state.destChest().orElse(null);
		}
	}

	@Override
	public TaskStatus tick() {
		Optional<PoscArea.Area2D> areaOpt = workArea();
		if (areaOpt.isEmpty()) {
			Paperwork.LOGGER.info("[Cook] no work area assigned");
			return TaskStatus.FAILURE;
		}
		PoscArea.Area2D area = areaOpt.get();
		if (!validateWorkArea(area)) {
			return TaskStatus.FAILURE;
		}

		switch (phase) {
			case SCAN_SOURCE -> {
				if (hasSmeltableInInventory()) {
					// Already holding raw ingredients: skip to the cook station.
					phase = Phase.SCAN_COOK_STATION;
					return TaskStatus.RUNNING;
				}
				if (sourceChest != null && chestHasSmeltable(sourceChest)) {
					currentTarget = sourceChest;
					phase = Phase.MOVE_TO_SOURCE;
					mob.navigateTo(sourceChest.x(), sourceChest.y(), sourceChest.z());
					return TaskStatus.RUNNING;
				}
				TilePosc source = nearestChestWithSmeltable(area);
				if (source == null) {
					Paperwork.LOGGER.info("[Cook] no source chest with smeltable items in area");
					return TaskStatus.FAILURE;
				}
				sourceChest = source;
				persistSourceChest(sourceChest);
				currentTarget = sourceChest;
				phase = Phase.MOVE_TO_SOURCE;
				mob.navigateTo(sourceChest.x(), sourceChest.y(), sourceChest.z());
				return TaskStatus.RUNNING;
			}
			case MOVE_TO_SOURCE -> {
				if (currentTarget == null) return TaskStatus.FAILURE;
				mob.navigateTo(currentTarget.x(), currentTarget.y(), currentTarget.z());
				if (mob.near(currentTarget, ACTION_REACH)) {
					mob.stopMoving();
					currentTarget = null;
					phase = Phase.TAKE_RAW;
				}
				return TaskStatus.RUNNING;
			}
			case TAKE_RAW -> {
				if (sourceChest == null) {
					Paperwork.LOGGER.info("[Cook] no source chest remembered");
					return TaskStatus.FAILURE;
				}
				TileEntityChest chest = MobWorldAccess.getChest(mob.world, sourceChest);
				if (chest == null) {
					Paperwork.LOGGER.info("[Cook] source chest at {} is gone", sourceChest);
					return TaskStatus.FAILURE;
				}
				ItemStack taken = takeSmeltableItem(chest);
				if (taken == null) {
					Paperwork.LOGGER.info("[Cook] source chest at {} has no smeltable item", sourceChest);
					return TaskStatus.FAILURE;
				}
				mob.inventory.insertItem(taken);
				chest.setChanged();
				phase = Phase.SCAN_COOK_STATION;
				return TaskStatus.RUNNING;
			}
			case SCAN_COOK_STATION -> {
				TilePosc station = nearestCookStation(area);
				if (station == null) {
					Paperwork.LOGGER.info("[Cook] no furnace or workbench in area");
					return TaskStatus.FAILURE;
				}
				cookStationPos = station;
				currentStationIsFurnace = isFurnace(station);
				currentTarget = station;
				phase = Phase.MOVE_TO_COOK;
				mob.navigateTo(station.x(), station.y(), station.z());
				return TaskStatus.RUNNING;
			}
			case MOVE_TO_COOK -> {
				if (currentTarget == null) return TaskStatus.FAILURE;
				mob.navigateTo(currentTarget.x(), currentTarget.y(), currentTarget.z());
				if (mob.near(currentTarget, ACTION_REACH)) {
					mob.stopMoving();
					currentTarget = null;
					phase = Phase.REFUEL_CHECK;
				}
				return TaskStatus.RUNNING;
			}
			case REFUEL_CHECK -> {
				if (cookStationPos == null) return TaskStatus.FAILURE;
				if (!currentStationIsFurnace) {
					// Workbench does not need fuel: proceed to cook.
					phase = Phase.COOKING;
					return TaskStatus.RUNNING;
				}
				if (furnaceHasFuel(cookStationPos)) {
					// Furnace already has fuel: proceed to cook.
					phase = Phase.COOKING;
					return TaskStatus.RUNNING;
				}
				// Furnace needs fuel: look for a chest with coal.
				TilePosc fuelSource = nearestChestWithCoal(area);
				if (fuelSource == null) {
					Paperwork.LOGGER.info("[Cook] no chest with coal in area to refuel furnace");
					return TaskStatus.FAILURE;
				}
				currentTarget = fuelSource;
				fuelChest = fuelSource;
				phase = Phase.SCAN_FUEL_SOURCE;
				return TaskStatus.RUNNING;
			}
			case SCAN_FUEL_SOURCE -> {
				if (currentTarget == null) return TaskStatus.FAILURE;
				mob.navigateTo(currentTarget.x(), currentTarget.y(), currentTarget.z());
				if (mob.near(currentTarget, ACTION_REACH)) {
					mob.stopMoving();
					currentTarget = null;
					phase = Phase.TAKE_FUEL;
				}
				return TaskStatus.RUNNING;
			}
			case TAKE_FUEL -> {
				if (fuelChest == null) {
					Paperwork.LOGGER.info("[Cook] lost track of the coal chest");
					return TaskStatus.FAILURE;
				}
				TileEntityChest chest = MobWorldAccess.getChest(mob.world, fuelChest);
				if (chest == null) {
					Paperwork.LOGGER.info("[Cook] coal chest at {} is gone", fuelChest);
					return TaskStatus.FAILURE;
				}
				ItemStack coal = takeOneItemOf(chest, Items.COAL);
				if (coal == null) {
					Paperwork.LOGGER.info("[Cook] coal chest at {} has no coal", fuelChest);
					return TaskStatus.FAILURE;
				}
				mob.inventory.insertItem(coal);
				chest.setChanged();
				fuelChest = null;
				// Return to the furnace to put the coal in.
				if (cookStationPos == null) return TaskStatus.FAILURE;
				currentTarget = cookStationPos;
				phase = Phase.REFUEL_FURNACE;
				mob.navigateTo(cookStationPos.x(), cookStationPos.y(), cookStationPos.z());
				return TaskStatus.RUNNING;
			}
			case REFUEL_FURNACE -> {
				if (cookStationPos == null) return TaskStatus.FAILURE;
				// Walk back to the furnace before putting the coal in.
				if (!mob.near(cookStationPos, ACTION_REACH)) {
					mob.navigateTo(cookStationPos.x(), cookStationPos.y(), cookStationPos.z());
					return TaskStatus.RUNNING;
				}
				mob.stopMoving();
				TileEntityFurnace furnace = getFurnace(cookStationPos);
				if (furnace == null) return TaskStatus.FAILURE;
				ItemStack coal = removeCoalFromInventory();
				if (coal == null) {
					Paperwork.LOGGER.info("[Cook] coal vanished from the inventory before refuelling");
					return TaskStatus.FAILURE;
				}
				ItemStack fuel = furnace.getItem(FURNACE_SLOT_FUEL);
				if (fuel == null) {
					furnace.setItem(FURNACE_SLOT_FUEL, coal);
				} else if (fuel.canStackWith(coal)) {
					fuel.stackSize += coal.stackSize;
					furnace.setItem(FURNACE_SLOT_FUEL, fuel);
				} else {
					// Another item is already in the fuel slot: put the coal
					// back instead of deleting it.
					mob.inventory.insertItem(coal);
					Paperwork.LOGGER.info("[Cook] furnace fuel slot holds another item, cannot refuel");
					return TaskStatus.FAILURE;
				}
				furnace.setChanged();
				phase = Phase.COOKING;
				return TaskStatus.RUNNING;
			}
			case COOKING -> {
				TileEntityFurnace furnace = cookStationPos != null ? getFurnace(cookStationPos) : null;
				if (furnace != null) {
					// Real furnace: leave one smeltable item in the ingredient
					// slot and let the world smelt it.
					ItemStack result = furnace.getItem(FURNACE_SLOT_RESULT);
					if (result != null && result.stackSize > 0) {
						takeFurnaceResult(furnace);
						phase = Phase.SCAN_DEST;
						return TaskStatus.RUNNING;
					}
					ItemStack ingredient = furnace.getItem(FURNACE_SLOT_INGREDIENT);
					if (ingredient == null || ingredient.stackSize <= 0) {
						ItemStack raw = removeSmeltableFromInventory();
						if (raw == null) {
							Paperwork.LOGGER.info("[Cook] no smeltable item in inventory to cook");
							return TaskStatus.FAILURE;
						}
						furnace.setItem(FURNACE_SLOT_INGREDIENT, raw);
						furnace.setChanged();
					}
					smeltTicks = 0;
					phase = Phase.WAIT_FOR_SMELT;
					return TaskStatus.RUNNING;
				}
				// Workbench station: no real smelting, produce the recipe
				// result directly as a fallback.
				ItemStack raw = removeSmeltableFromInventory();
				if (raw == null) {
					Paperwork.LOGGER.info("[Cook] no smeltable item in inventory to cook");
					return TaskStatus.FAILURE;
				}
				ItemStack cooked = furnaceResultFor(raw);
				if (cooked == null) cooked = raw.copy();
				cooked.stackSize = 1;
				markCooked(cooked);
				mob.inventory.insertItem(cooked);
				phase = Phase.SCAN_DEST;
				return TaskStatus.RUNNING;
			}
			case WAIT_FOR_SMELT -> {
				if (cookStationPos == null) return TaskStatus.FAILURE;
				TileEntityFurnace furnace = getFurnace(cookStationPos);
				if (furnace == null) {
					Paperwork.LOGGER.info("[Cook] furnace at {} is gone", cookStationPos);
					return TaskStatus.FAILURE;
				}
				ItemStack result = furnace.getItem(FURNACE_SLOT_RESULT);
				if (result != null && result.stackSize > 0) {
					takeFurnaceResult(furnace);
					phase = Phase.SCAN_DEST;
					return TaskStatus.RUNNING;
				}
				smeltTicks++;
				if (smeltTicks > SMELT_TIMEOUT_TICKS) {
					Paperwork.LOGGER.info("[Cook] furnace at {} did not smelt the item in time", cookStationPos);
					// Give the stuck ingredient back instead of deleting it.
					ItemStack stuck = furnace.getItem(FURNACE_SLOT_INGREDIENT);
					if (stuck != null && stuck.stackSize > 0) {
						furnace.setItem(FURNACE_SLOT_INGREDIENT, null);
						mob.inventory.insertItem(stuck);
						furnace.setChanged();
					}
					return TaskStatus.FAILURE;
				}
				return TaskStatus.RUNNING;
			}
			case SCAN_DEST -> {
				TilePosc dest = (destChest != null && !destChest.equals(sourceChest))
					? destChest
					: nearestOtherChest(area, sourceChest);
				if (dest == null) {
					// No destination chest yet: keep the cooked food in the
					// inventory and wait until one appears.
					phase = Phase.WAITING_FOR_DEST;
					return TaskStatus.RUNNING;
				}
				destChest = dest;
				persistDestChest(destChest);
				currentTarget = destChest;
				phase = Phase.MOVE_TO_DEST;
				mob.navigateTo(destChest.x(), destChest.y(), destChest.z());
				return TaskStatus.RUNNING;
			}
			case WAITING_FOR_DEST -> {
				TilePosc dest = nearestOtherChest(area, sourceChest);
				if (dest != null) {
					destChest = dest;
					persistDestChest(destChest);
					currentTarget = destChest;
					phase = Phase.MOVE_TO_DEST;
					mob.navigateTo(destChest.x(), destChest.y(), destChest.z());
				}
				return TaskStatus.RUNNING;
			}
			case MOVE_TO_DEST -> {
				if (currentTarget == null) return TaskStatus.FAILURE;
				mob.navigateTo(currentTarget.x(), currentTarget.y(), currentTarget.z());
				if (mob.near(currentTarget, ACTION_REACH)) {
					mob.stopMoving();
					currentTarget = null;
					phase = Phase.DEPOSIT;
				}
				return TaskStatus.RUNNING;
			}
			case DEPOSIT -> {
				if (destChest == null) return TaskStatus.FAILURE;
				TileEntityChest chest = MobWorldAccess.getChest(mob.world, destChest);
				if (chest == null) return TaskStatus.FAILURE;
				ItemStack cooked = removeCookedFromInventory();
				if (cooked == null) return TaskStatus.FAILURE;
				insertIntoChest(chest, cooked);
				chest.setChanged();
				phase = Phase.SCAN_SOURCE;
				cookStationPos = null;
				currentStationIsFurnace = false;
				return TaskStatus.RUNNING;
			}
		}
		return TaskStatus.FAILURE;
	}

	@Override
	public void stop(StopReason reason) {
		Paperwork.LOGGER.info("[Cook] stop ({}) — clearing target", reason);
		currentTarget = null;
		cookStationPos = null;
		currentStationIsFurnace = false;
		mob.stopMoving();
	}

	@Override
	public boolean interruptible() {
		return Task.super.interruptible();
	}

	// ------------------------------------------------------------------ helpers

	private Optional<PoscArea.Area2D> workArea() {
		return mob.workArea;
	}

	/// A valid kitchen is between 3 and 16 blocks on each side, with the long
	/// side at least 6. The area can be marked in either orientation, so a
	/// 6x4 plot (4 wide, 6 long) is valid too.
	public static boolean isValidSize(PoscArea.Area2D area) {
		int width = Math.abs(area.a().x() - area.b().x()) + 1;
		int length = Math.abs(area.a().z() - area.b().z()) + 1;
		if (width > MAX_SIDE || length > MAX_SIDE) return false;
		return (width >= MIN_WIDTH && length >= MIN_LENGTH)
			|| (width >= MIN_LENGTH && length >= MIN_WIDTH);
	}

	/// Reports an invalid kitchen size once and tells the caller the area is
	/// unusable, so the task fails instead of looping and spamming the log.
	private boolean validateWorkArea(PoscArea.Area2D area) {
		if (isValidSize(area)) {
			return true;
		}
		if (!sizeWarned) {
			sizeWarned = true;
			int width = Math.abs(area.a().x() - area.b().x()) + 1;
			int length = Math.abs(area.a().z() - area.b().z()) + 1;
			Paperwork.LOGGER.info("[Cook] work area {}x{} out of bounds (min {}x{} or {}x{}, max {}x{})",
				width, length, MIN_WIDTH, MIN_LENGTH, MIN_LENGTH, MIN_WIDTH, MAX_SIDE, MAX_SIDE);
		}
		return false;
	}

	/// Whether the chef is holding at least one item a furnace can smelt.
	private boolean hasSmeltableInInventory() {
		for (int i = 0; i < mob.inventory.getContainerSize(); i++) {
			ItemStack stack = mob.inventory.getItem(i);
			if (stack != null && stack.stackSize > 0 && !isCooked(stack) && isSmeltable(stack)) {
				return true;
			}
		}
		return false;
	}

	/// Whether an item stack has a furnace recipe.
	private static boolean isSmeltable(ItemStack stack) {
		return furnaceResultFor(stack) != null;
	}

	/// The furnace output for an input stack, or null when there is no recipe.
	@Nullable
	private static ItemStack furnaceResultFor(ItemStack input) {
		if (input == null || input.stackSize <= 0) return null;
		for (RecipeEntryFurnace recipe : Registries.RECIPES.getAllFurnaceRecipes()) {
			if (recipe != null && recipe.matches(input)) {
				ItemStack output = recipe.getOutput();
				return output == null ? null : output.copy();
			}
		}
		return null;
	}

	/// Removes one unit of the first smeltable item from the inventory.
	@Nullable
	private ItemStack removeSmeltableFromInventory() {
		for (int i = 0; i < mob.inventory.getContainerSize(); i++) {
			ItemStack stack = mob.inventory.getItem(i);
			if (stack != null && stack.stackSize > 0 && !isCooked(stack) && isSmeltable(stack)) {
				ItemStack part = stack.copy();
				part.stackSize = 1;
				stack.stackSize--;
				if (stack.stackSize <= 0) {
					mob.inventory.setItem(i, null);
				}
				return part;
			}
		}
		return null;
	}

	/// Removes one piece of coal from the inventory.
	@Nullable
	private ItemStack removeCoalFromInventory() {
		for (int i = 0; i < mob.inventory.getContainerSize(); i++) {
			ItemStack stack = mob.inventory.getItem(i);
			if (stack != null && stack.stackSize > 0 && stack.getItem().equals(Items.COAL)) {
				ItemStack part = stack.copy();
				part.stackSize = 1;
				stack.stackSize--;
				if (stack.stackSize <= 0) {
					mob.inventory.setItem(i, null);
				}
				return part;
			}
		}
		return null;
	}

	/// Removes one unit of the first cooked item stack from the inventory.
	private ItemStack removeCookedFromInventory() {
		for (int i = 0; i < mob.inventory.getContainerSize(); i++) {
			ItemStack stack = mob.inventory.getItem(i);
			if (stack != null && stack.stackSize > 0 && isCooked(stack)) {
				ItemStack copy = stack.copy();
				copy.stackSize = 1;
				stack.stackSize--;
				if (stack.stackSize <= 0) {
					mob.inventory.setItem(i, null);
				}
				return copy;
			}
		}
		// Tolerant fallback: take any item if none is explicitly marked cooked.
		return removeOneFromInventory();
	}

	/// Removes one unit of the first item stack from the inventory (any kind).
	private ItemStack removeOneFromInventory() {
		for (int i = 0; i < mob.inventory.getContainerSize(); i++) {
			ItemStack stack = mob.inventory.getItem(i);
			if (stack != null && stack.stackSize > 0) {
				ItemStack copy = stack.copy();
				copy.stackSize = 1;
				stack.stackSize--;
				if (stack.stackSize <= 0) {
					mob.inventory.setItem(i, null);
				}
				return copy;
			}
		}
		return null;
	}

	/// Marks an item stack as cooked by writing an NBT flag.
	private void markCooked(ItemStack stack) {
		CompoundTag data = stack.getData();
		if (data == null) {
			data = new CompoundTag();
			stack.setData(data);
		}
		data.putBoolean(NBT_COOKED, true);
	}

	/// Whether an item stack carries the "cooked" NBT flag.
	private static boolean isCooked(ItemStack stack) {
		CompoundTag data = stack.getData();
		return data != null && data.getBoolean(NBT_COOKED);
	}

	/// Takes one unit of a smeltable item from a chest, skipping anything the
	/// furnace cannot cook.
	@Nullable
	private ItemStack takeSmeltableItem(TileEntityChest chest) {
		for (int i = 0; i < chest.getContainerSize(); i++) {
			ItemStack stack = chest.getItem(i);
			if (stack != null && stack.stackSize > 0 && isSmeltable(stack)) {
				ItemStack part = stack.copy();
				part.stackSize = 1;
				stack.stackSize--;
				if (stack.stackSize <= 0) {
					chest.setItem(i, null);
				}
				return part;
			}
		}
		return null;
	}

	/// Moves one unit from the furnace result slot into the chef's inventory.
	private void takeFurnaceResult(TileEntityFurnace furnace) {
		ItemStack result = furnace.getItem(FURNACE_SLOT_RESULT);
		if (result == null || result.stackSize <= 0) return;
		ItemStack part = result.copy();
		part.stackSize = 1;
		result.stackSize--;
		furnace.setItem(FURNACE_SLOT_RESULT, result.stackSize <= 0 ? null : result);
		markCooked(part);
		mob.inventory.insertItem(part);
		furnace.setChanged();
	}

	/// Takes one unit of a specific item kind from a chest.
	private ItemStack takeOneItemOf(TileEntityChest chest, net.minecraft.core.item.Item item) {
		if (item == null) return null;
		for (int i = 0; i < chest.getContainerSize(); i++) {
			ItemStack stack = chest.getItem(i);
			if (stack != null && stack.stackSize > 0 && stack.getItem().equals(item)) {
				ItemStack part = stack.copy();
				part.stackSize = 1;
				stack.stackSize--;
				if (stack.stackSize <= 0) {
					chest.setItem(i, null);
				}
				return part;
			}
		}
		return null;
	}

	private void insertIntoChest(TileEntityChest chest, ItemStack stack) {
		// Try to stack with existing items first.
		for (int i = 0; i < chest.getContainerSize(); i++) {
			ItemStack existing = chest.getItem(i);
			if (existing != null && existing.canStackWith(stack)) {
				int space = existing.getMaxStackSize() - existing.stackSize;
				int transfer = Math.min(stack.stackSize, space);
				existing.stackSize += transfer;
				stack.stackSize -= transfer;
				if (stack.stackSize <= 0) {
					return;
				}
			}
		}
		// Otherwise put it in an empty slot.
		for (int i = 0; i < chest.getContainerSize(); i++) {
			if (chest.getItem(i) == null) {
				int transfer = Math.min(stack.stackSize, stack.getMaxStackSize());
				ItemStack copy = stack.copy();
				copy.stackSize = transfer;
				chest.setItem(i, copy);
				stack.stackSize -= transfer;
				if (stack.stackSize <= 0) {
					return;
				}
			}
		}
	}

	private boolean chestHasSmeltable(TilePosc pos) {
		TileEntityChest chest = MobWorldAccess.getChest(mob.world, pos);
		if (chest == null) return false;
		for (int i = 0; i < chest.getContainerSize(); i++) {
			ItemStack stack = chest.getItem(i);
			if (stack != null && stack.stackSize > 0 && isSmeltable(stack)) return true;
		}
		return false;
	}

	private boolean chestHasCoal(TilePosc pos) {
		TileEntityChest chest = MobWorldAccess.getChest(mob.world, pos);
		if (chest == null) return false;
		for (int i = 0; i < chest.getContainerSize(); i++) {
			ItemStack stack = chest.getItem(i);
			if (stack != null && stack.stackSize > 0 && stack.getItem().equals(Items.COAL)) {
				return true;
			}
		}
		return false;
	}

	private TilePosc nearestChestWithSmeltable(PoscArea.Area2D area) {
		TilePosc best = null;
		double bestDist = Double.MAX_VALUE;
		double mx = mob.x, mz = mob.z;
		for (TilePosc p : MobWorldAccess.getBlocksInArea3D(area)) {
			if (!chestHasSmeltable(p)) continue;
			double dx = p.x() + 0.5 - mx;
			double dz = p.z() + 0.5 - mz;
			double dist = dx * dx + dz * dz;
			if (dist < bestDist) {
				bestDist = dist;
				best = p;
			}
		}
		return best;
	}

	private TilePosc nearestChestWithCoal(PoscArea.Area2D area) {
		TilePosc best = null;
		double bestDist = Double.MAX_VALUE;
		double mx = mob.x, mz = mob.z;
		for (TilePosc p : MobWorldAccess.getBlocksInArea3D(area)) {
			if (!chestHasCoal(p)) continue;
			double dx = p.x() + 0.5 - mx;
			double dz = p.z() + 0.5 - mz;
			double dist = dx * dx + dz * dz;
			if (dist < bestDist) {
				bestDist = dist;
				best = p;
			}
		}
		return best;
	}

	private boolean isFurnace(TilePosc pos) {
		int id = mob.world.getBlockType(pos).id();
		return id == Blocks.FURNACE_STONE_IDLE.id() || id == Blocks.FURNACE_STONE_ACTIVE.id();
	}

	private boolean furnaceHasFuel(TilePosc pos) {
		TileEntityFurnace furnace = getFurnace(pos);
		if (furnace == null) return false;
		ItemStack fuel = furnace.getItem(FURNACE_SLOT_FUEL);
		return fuel != null && fuel.stackSize > 0;
	}

	@Nullable
	private TileEntityFurnace getFurnace(TilePosc pos) {
		net.minecraft.core.block.entity.TileEntity te = mob.world.getTileEntity(pos);
		if (te instanceof TileEntityFurnace furnace) {
			return furnace;
		}
		return null;
	}

	/// The closest cook station: a furnace is always preferred because it can
	/// smelt for real; the workbench is only a fallback.
	private TilePosc nearestCookStation(PoscArea.Area2D area) {
		TilePosc furnace = nearestMatching(area, this::isFurnace);
		if (furnace != null) return furnace;
		return nearestMatching(area, p -> mob.world.getBlockType(p).id() == Blocks.WORKBENCH.id());
	}

	private TilePosc nearestMatching(PoscArea.Area2D area, java.util.function.Predicate<TilePosc> matches) {
		TilePosc best = null;
		double bestDist = Double.MAX_VALUE;
		double mx = mob.x, mz = mob.z;
		for (TilePosc p : MobWorldAccess.getBlocksInArea3D(area)) {
			if (!matches.test(p)) continue;
			double dx = p.x() + 0.5 - mx;
			double dz = p.z() + 0.5 - mz;
			double dist = dx * dx + dz * dz;
			if (dist < bestDist) {
				bestDist = dist;
				best = p;
			}
		}
		return best;
	}

	private TilePosc nearestOtherChest(PoscArea.Area2D area, TilePosc exclude) {
		TilePosc best = null;
		double bestDist = Double.MAX_VALUE;
		double mx = mob.x, mz = mob.z;
		for (TilePosc p : MobWorldAccess.getBlocksInArea3D(area)) {
			if (exclude != null && p.equals(exclude)) continue;
			TileEntityChest chest = MobWorldAccess.getChest(mob.world, p);
			if (chest == null) continue;
			double dx = p.x() + 0.5 - mx;
			double dz = p.z() + 0.5 - mz;
			double dist = dx * dx + dz * dz;
			if (dist < bestDist) {
				bestDist = dist;
				best = p;
			}
		}
		return best;
	}

	private void persistSourceChest(TilePosc pos) {
		if (mob.jobState() instanceof ChefJobState state) {
			state.setSourceChest(pos);
		}
	}

	private void persistDestChest(TilePosc pos) {
		if (mob.jobState() instanceof ChefJobState state) {
			state.setDestChest(pos);
		}
	}

	private enum Phase {
		SCAN_SOURCE, MOVE_TO_SOURCE, TAKE_RAW,
		SCAN_COOK_STATION, MOVE_TO_COOK, REFUEL_CHECK,
		SCAN_FUEL_SOURCE, MOVE_TO_FUEL_SOURCE, TAKE_FUEL, REFUEL_FURNACE,
		COOKING, WAIT_FOR_SMELT,
		SCAN_DEST, WAITING_FOR_DEST, MOVE_TO_DEST, DEPOSIT
	}
}
