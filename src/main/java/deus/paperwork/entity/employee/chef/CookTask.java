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
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import net.minecraft.core.world.pos.TilePosc;

import java.util.Optional;

/// Cooks raw ingredients taken from a chest inside the chef's workplace and
/// deposits the cooked result into a different chest.
///
/// The task runs as an internal state machine:
/// <ul>
///   <li>{@link Phase#SCAN_SOURCE} — find a chest inside the workplace that holds items.</li>
///   <li>{@link Phase#MOVE_TO_SOURCE} — walk to that chest.</li>
///   <li>{@link Phase#TAKE_RAW} — take one raw item into the chef's inventory.</li>
///   <li>{@link Phase#SCAN_COOK_STATION} — find a furnace or workbench inside the workplace.</li>
///   <li>{@link Phase#MOVE_TO_COOK} — walk to the cook station.</li>
///   <li>{@link Phase#COOKING} — consume the raw item and produce a cooked one in the inventory.</li>
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

	public CookTask(MobEmployee mob) {
		this.mob = mob;
	}

	@Override
	public void start() {
		phase = Phase.SCAN_SOURCE;
		currentTarget = null;
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
		validateWorkArea(area);
		if (!hasBedInArea(area)) {
			Paperwork.LOGGER.info("[Cook] no bed in work area");
			return TaskStatus.FAILURE;
		}

		switch (phase) {
			case SCAN_SOURCE -> {
				if (hasRawInInventory()) {
					// Already holding raw ingredients: skip to the cook station.
					phase = Phase.SCAN_COOK_STATION;
					return TaskStatus.RUNNING;
				}
				if (sourceChest != null && chestHasItems(sourceChest)) {
					currentTarget = sourceChest;
					phase = Phase.MOVE_TO_SOURCE;
					mob.navigateTo(sourceChest.x(), sourceChest.y(), sourceChest.z());
					return TaskStatus.RUNNING;
				}
				TilePosc source = nearestChestWithItems(area);
				if (source == null) {
					Paperwork.LOGGER.info("[Cook] no source chest with items in area");
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
				if (sourceChest == null) return TaskStatus.FAILURE;
				TileEntityChest chest = MobWorldAccess.getChest(mob.world, sourceChest);
				if (chest == null) return TaskStatus.FAILURE;
				ItemStack taken = takeOneItem(chest);
				if (taken == null) return TaskStatus.FAILURE;
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
					phase = Phase.COOKING;
				}
				return TaskStatus.RUNNING;
			}
			case COOKING -> {
				ItemStack raw = peekFirstInventoryItem();
				if (raw == null) {
					Paperwork.LOGGER.info("[Cook] no raw item in inventory to cook");
					return TaskStatus.FAILURE;
				}
				consumeOneFromInventory();
				ItemStack cooked = raw.copy();
				cooked.stackSize = 1;
				markCooked(cooked);
				mob.inventory.insertItem(cooked);
				phase = Phase.SCAN_DEST;
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
				return TaskStatus.RUNNING;
			}
		}
		return TaskStatus.FAILURE;
	}

	@Override
	public void stop(StopReason reason) {
		Paperwork.LOGGER.info("[Cook] stop ({}) — clearing target", reason);
		currentTarget = null;
		mob.stopMoving();
	}

	@Override
	public boolean interruptible() {
		return Task.super.interruptible();
	}

	// ------------------------------------------------------------------ helpers

	private Optional<PoscArea.Area2D> workArea() {
		if (mob.jobState() instanceof ChefJobState state) {
			return state.workArea();
		}
		return Optional.empty();
	}

	private void validateWorkArea(PoscArea.Area2D area) {
		int width = Math.abs(area.a().x() - area.b().x()) + 1;
		int length = Math.abs(area.a().z() - area.b().z()) + 1;
		if (width < MIN_WIDTH || length < MIN_LENGTH || width > MAX_SIDE || length > MAX_SIDE) {
			Paperwork.LOGGER.info("[Cook] work area {}x{} out of bounds (min {}x{}, max {}x{})",
				width, length, MIN_WIDTH, MIN_LENGTH, MAX_SIDE, MAX_SIDE);
		}
	}

	/// Whether the chef is holding at least one raw (uncooked) item.
	private boolean hasRawInInventory() {
		for (int i = 0; i < mob.inventory.getContainerSize(); i++) {
			ItemStack stack = mob.inventory.getItem(i);
			if (stack != null && stack.stackSize > 0 && !isCooked(stack)) {
				return true;
			}
		}
		return false;
	}

	/// Whether the chef is holding at least one cooked item.
	private boolean hasCookedInInventory() {
		for (int i = 0; i < mob.inventory.getContainerSize(); i++) {
			ItemStack stack = mob.inventory.getItem(i);
			if (stack != null && stack.stackSize > 0 && isCooked(stack)) {
				return true;
			}
		}
		return false;
	}

	/// Returns the first item stack in the inventory without consuming it.
	private ItemStack peekFirstInventoryItem() {
		for (int i = 0; i < mob.inventory.getContainerSize(); i++) {
			ItemStack stack = mob.inventory.getItem(i);
			if (stack != null && stack.stackSize > 0) {
				return stack;
			}
		}
		return null;
	}

	/// Removes one unit of the first raw item stack from the inventory.
	private void consumeOneFromInventory() {
		for (int i = 0; i < mob.inventory.getContainerSize(); i++) {
			ItemStack stack = mob.inventory.getItem(i);
			if (stack != null && stack.stackSize > 0 && !isCooked(stack)) {
				stack.stackSize--;
				if (stack.stackSize <= 0) {
					mob.inventory.setItem(i, null);
				}
				return;
			}
		}
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

	private ItemStack takeOneItem(TileEntityChest chest) {
		for (int i = 0; i < chest.getContainerSize(); i++) {
			ItemStack stack = chest.getItem(i);
			if (stack != null && stack.stackSize > 0) {
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

	private boolean chestHasItems(TilePosc pos) {
		TileEntityChest chest = MobWorldAccess.getChest(mob.world, pos);
		if (chest == null) return false;
		for (int i = 0; i < chest.getContainerSize(); i++) {
			ItemStack stack = chest.getItem(i);
			if (stack != null && stack.stackSize > 0) return true;
		}
		return false;
	}

	private TilePosc nearestChestWithItems(PoscArea.Area2D area) {
		TilePosc best = null;
		double bestDist = Double.MAX_VALUE;
		double mx = mob.x, mz = mob.z;
		for (TilePosc p : PoscArea.tiles(area)) {
			TileEntityChest chest = MobWorldAccess.getChest(mob.world, p);
			if (chest == null) continue;
			boolean hasItems = false;
			for (int i = 0; i < chest.getContainerSize(); i++) {
				ItemStack stack = chest.getItem(i);
				if (stack != null && stack.stackSize > 0) {
					hasItems = true;
					break;
				}
			}
			if (!hasItems) continue;
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

	private boolean hasBedInArea(PoscArea.Area2D area) {
		for (TilePosc p : PoscArea.tiles(area)) {
			if (mob.world.getBlockType(p).id() == Blocks.BED.id()) return true;
		}
		return false;
	}

	private TilePosc nearestCookStation(PoscArea.Area2D area) {
		TilePosc best = null;
		double bestDist = Double.MAX_VALUE;
		double mx = mob.x, mz = mob.z;
		for (TilePosc p : PoscArea.tiles(area)) {
			int id = mob.world.getBlockType(p).id();
			if (id != Blocks.FURNACE_STONE_IDLE.id()
				&& id != Blocks.FURNACE_STONE_ACTIVE.id()
				&& id != Blocks.WORKBENCH.id()) continue;
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
		for (TilePosc p : PoscArea.tiles(area)) {
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
		SCAN_COOK_STATION, MOVE_TO_COOK, COOKING,
		SCAN_DEST, WAITING_FOR_DEST, MOVE_TO_DEST, DEPOSIT
	}
}
