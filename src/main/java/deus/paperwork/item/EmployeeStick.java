package deus.paperwork.item;

import deus.brainless.Brainless;
import deus.paperwork.entity.employee.MobEmployee;
import deus.paperwork.entity.employee.chef.ChefJob;
import deus.paperwork.entity.employee.chef.ChefJobState;
import deus.paperwork.entity.employee.farmer.FarmerJob;
import deus.paperwork.entity.employee.farmer.FarmerJobState;
import deus.paperwork.entity.employee.guard.GuardJob;
import deus.paperwork.entity.employee.guard.GuardWeapon;
import deus.paperwork.entity.employee.job.EmployeeJobs;
import deus.paperwork.core.util.PoscArea;
import deus.utils.annotations.RegisterItemModel;
import net.minecraft.client.render.item.model.ItemModelStandard;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import net.minecraft.core.net.command.TextFormatting;
import net.minecraft.core.util.helper.Side;
import net.minecraft.core.world.World;
import net.minecraft.core.world.pos.TilePos;
import net.minecraft.core.world.pos.TilePosc;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RegisterItemModel(model = ItemModelStandard.class)
public class EmployeeStick extends Item {

	public enum AssignMode { BED, FOOD }

	/// Cama guardada con shift+click sobre una cama, esperando a ser asignada a un empleado.
	private TilePosc pendingBed = null;

	/// Puntos del área marcados con click/shift+click sobre troncos de roble.
	private TilePosc areaPointA = null;
	private TilePosc areaPointB = null;

	public EmployeeStick(@NotNull String translationKey, @NotNull String namespaceId, int id) {
		super(translationKey, namespaceId, id);
	}

	@Override
	public boolean onUseOnBlock(@NotNull ItemStack selfStack, @NotNull World world,
	                            @Nullable Player player, @NotNull TilePosc blockPos,
	                            @NotNull Side side, double xHit, double yHit) {
		if (world.isClientSide || player == null) return false;

		int blockId = world.getBlockType(blockPos).id();
		TilePosc pos = new TilePos(blockPos.x(), blockPos.y(), blockPos.z());

		if (blockId == Blocks.BED.id()) {
			if (player.isSneaking()) {
				pendingBed = pos;
				areaPointA = null;
				areaPointB = null;
				Brainless.LOGGER.info("[Stick] Bed position saved: {}", pos);
				say(player, TextFormatting.Base.YELLOW, "Employee stick: bed saved " + fmt(pos)
					+ " — right-click an employee to assign it.");
			} else {
				// Click normal sobre cama: también guarda (atajo rápido).
				pendingBed = pos;
				areaPointA = null;
				areaPointB = null;
				Brainless.LOGGER.info("[Stick] Bed position saved: {}", pos);
				say(player, TextFormatting.Base.YELLOW, "Employee stick: bed saved " + fmt(pos)
					+ " — right-click an employee to assign it.");
			}
			return true;

		} else if (blockId == Blocks.LOG_OAK.id()) {
			if (player.isSneaking()) {
				areaPointB = pos;
				pendingBed = null;
				Brainless.LOGGER.info("[Stick] Area point B set: {}", pos);
				say(player, TextFormatting.Base.YELLOW, "Area: point B set " + fmt(pos) + ".");
				if (areaPointA != null) {
					Brainless.LOGGER.info("[Stick] Area ready: {} -> {}", areaPointA, areaPointB);
					say(player, TextFormatting.Base.GREEN, "Area ready — right-click an employee to assign it.");
				}
			} else {
				areaPointA = pos;
				areaPointB = null;
				pendingBed = null;
				Brainless.LOGGER.info("[Stick] Area point A set: {}", pos);
				say(player, TextFormatting.Base.YELLOW, "Area: point A set " + fmt(pos)
					+ " — sneak+click the second log to finish the area.");
			}
			return true;

		} else {
			Brainless.LOGGER.info("[Stick] Block not recognized for assignment");
			say(player, TextFormatting.Base.RED, "Employee stick: click a bed (shift) or oak logs (click/shift) to mark points.");
			return false;
		}
	}

	/// Entrega un arma (espada o arco) al empleado si es un guardia.
	/// Se consume una unidad del ítem que el jugador trae en la mano.
	private boolean giveWeapon(MobEmployee employee, ItemStack held, Player player) {
		if (!(employee.job() instanceof GuardJob)) {
			return false;
		}
		Item item = held.getItem();
		GuardWeapon weapon;
		if (GuardJob.isBow(item)) {
			weapon = GuardWeapon.BOW;
		} else if (GuardJob.isSword(held)) {
			weapon = GuardWeapon.SWORD;
		} else {
			return false;
		}

		// Busca un hueco libre en el inventario del empleado y guarda una copia.
		int slot = -1;
		for (int i = 0; i < employee.inventory.getContainerSize(); i++) {
			if (employee.inventory.getItem(i) == null) {
				slot = i;
				break;
			}
		}
		if (slot < 0) {
			say(player, TextFormatting.Base.RED, "Employee's inventory is full.");
			return false;
		}

		ItemStack copy = held.copy();
		copy.stackSize = 1;
		employee.inventory.setItem(slot, copy);

		// Consume una unidad del ítem en la mano del jugador.
		held.stackSize--;
		if (held.stackSize <= 0) {
			held.stackSize = 0;
		}

		employee.equipGuardWeapon(weapon, slot);
		Brainless.LOGGER.info("[Stick] Gave {} to guard at slot {}", weapon, slot);
		if (!employee.world.isClientSide && player != null) {
			say(player, TextFormatting.Base.GREEN, "Guard equipped with a " + weapon + ".");
		}
		return true;
	}

	@Override
	public boolean useOnEntity(@NotNull ItemStack selfStack, @NotNull Player player, @NotNull Mob mob) {
		if (!(mob instanceof MobEmployee employee)) return false;

		// 0) Entregar un arma al guardia si el jugador trae espada o arco en la mano.
		if (selfStack != null && selfStack.stackSize > 0 && giveWeapon(employee, selfStack, player)) {
			return true;
		}

		boolean assigned = false;

		// 1) Asignar cama pendiente si la hay.
		if (pendingBed != null) {
			employee.assignBed(pendingBed);
			Brainless.LOGGER.info("[Stick] Bed assigned to employee at {}", pendingBed);
			if (!player.world.isClientSide) {
				say(player, TextFormatting.Base.GREEN, "Employee got a bed at " + fmt(pendingBed) + ".");
			}
			pendingBed = null;
			assigned = true;
		}

		// 2) Asignar área pendiente si la hay.
		// Siempre se asigna como foodPlace para que cualquier empleado pueda
		// ir a comer cuando tenga hambre. Además, según el job, se guarda como
		// workArea (chef) o farmArea (farmer) para su tarea específica.
		if (areaPointA != null && areaPointB != null) {
			PoscArea.Area2D area = new PoscArea.Area2D(areaPointA, areaPointB);
			employee.assignFoodArea(area);

			if (employee.job() instanceof ChefJob) {
				ChefJobState state = (ChefJobState) employee.jobState();
				state.setWorkArea(area);
				Brainless.LOGGER.info("[Stick] Chef work area assigned: {} -> {}", areaPointA, areaPointB);
				if (!player.world.isClientSide) {
					say(player, TextFormatting.Base.GREEN, "Chef got a food+work area " + fmt(areaPointA) + " -> " + fmt(areaPointB) + ".");
				}
			} else if (employee.job() instanceof FarmerJob) {
				FarmerJobState state = (FarmerJobState) employee.jobState();
				state.setFarmArea(area);
				Brainless.LOGGER.info("[Stick] Farmer farm area assigned: {} -> {}", areaPointA, areaPointB);
				if (!player.world.isClientSide) {
					say(player, TextFormatting.Base.GREEN, "Farmer got a food+farm area " + fmt(areaPointA) + " -> " + fmt(areaPointB) + ".");
				}
			} else {
				Brainless.LOGGER.info("[Stick] Food area assigned to plain employee: {} -> {}", areaPointA, areaPointB);
				if (!player.world.isClientSide) {
					say(player, TextFormatting.Base.GREEN, "Employee got a food area " + fmt(areaPointA) + " -> " + fmt(areaPointB) + ".");
				}
			}
			areaPointA = null;
			areaPointB = null;
			assigned = true;
		}

		// 3) Si no había nada pendiente, ciclar la profesión del empleado.
		if (!assigned) {
			cycleJob(employee, player);
		}

		return assigned;
	}

	/// Cicla la profesión del empleado: plain → farmer → chef → guard → plain.
	private void cycleJob(MobEmployee employee, Player player) {
		String current = employee.jobId();
		String next;
		if (EmployeeJobs.PLAIN_ID.equals(current)) {
			next = EmployeeJobs.FARMER_ID;
		} else if (EmployeeJobs.FARMER_ID.equals(current)) {
			next = ChefJob.CHEF_ID;
		} else if (ChefJob.CHEF_ID.equals(current)) {
			next = GuardJob.GUARD_ID;
		} else {
			next = EmployeeJobs.PLAIN_ID;
		}
		employee.setJob(next);
		Brainless.LOGGER.info("[Stick] Employee job changed: {} -> {}", current, next);
		if (!employee.world.isClientSide && player != null) {
			say(player, TextFormatting.Base.GREEN, "Employee is now a " + next + ".");
		}
	}

	private static String fmt(TilePosc pos) {
		return pos.x() + "/" + pos.y() + "/" + pos.z();
	}

	private void say(Player player, TextFormatting.Base color, String message) {
		if (player != null) {
			player.sendMessage(color, "[Employee] " + message);
		}
	}
}
