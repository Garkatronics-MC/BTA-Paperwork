package deus.paperwork.entity.employee.guard;

import deus.brainless.agent.Agent;
import deus.brainless.goap.GoapAction;
import deus.paperwork.core.util.PoscArea;
import deus.paperwork.entity.employee.MobEmployee;
import deus.paperwork.entity.employee.enums.EmployeeAlert;
import deus.paperwork.entity.employee.job.EmployeeAccessory;
import deus.paperwork.entity.employee.job.EmployeeJob;
import deus.paperwork.entity.employee.job.EmployeeJobState;
import deus.paperwork.entity.employee.job.EmployeeJobs;
import deus.paperwork.entity.employee.job.EmployeeModelSpec;
import deus.paperwork.entity.employee.model.EmployeeStats;
import deus.paperwork.entity.employee.model.EmployeeStations;
import deus.paperwork.entity.employee.model.EmployeeTraits;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;

import java.util.List;
import java.util.Optional;

/// El oficio guard: un empleado que persigue y ataca a los mobs hostiles.
///
/// El guardia puede llevar equipada una espada o un arco (del inventario del
/// empleado, entregada por un jugador con clic derecho). Según el arma, el
/// tipo de objetivo (creeper obliga a distancia) y la distancia al objetivo,
/// el cerebro alterna entre pelea cuerpo a cuerpo y pelea a distancia.
///
/// El oficio es un singleton sin estado: todo lo mutable vive en el
/// {@link GuardJobState} del mob.
public final class GuardJob implements EmployeeJob {

	/// Stable id del oficio ("guard"), persistido en NBT y sincronizado al cliente.
	public static final String GUARD_ID = EmployeeJobs.GUARD_ID;

	@Override
	public String id() {
		return EmployeeJobs.GUARD_ID;
	}

	/// El guardia no tiene accesorio de modelo todavía: lleva el aspecto base del empleado.
	@Override
	public List<EmployeeAccessory> accessories() {
		return List.of();
	}

	/// El esqueleto base no tiene huesos extra, no hay nada que alternar.
	@Override
	public EmployeeModelSpec modelSpec(MobEmployee mob) {
		return EmployeeModelSpec.empty();
	}

	@Override
	public Agent buildAgent(MobEmployee mob, EmployeeTraits traits, EmployeeStats stats, EmployeeStations stations) {
		return GuardAgent.build(mob, traits, stats, stations);
	}

	/// Las estaciones de un guardia son las base: no necesita zona de trabajo.
	@Override
	public EmployeeStations stations(MobEmployee mob, EmployeeStations base) {
		return base;
	}

	/// El guardia no recarga nada especial: pelea donde esté.
	@Override
	public void recharge(MobEmployee mob, EmployeeStats stats) {}

	/// Lee el mundo antes de que el cerebro evalúe:
	/// <ul>
	///   <li>Elige (o conserva) el objetivo hostil más cercano.</li>
	///   <li>Determina el modo de combate (cuerpo a cuerpo / a distancia)
	///       según el arma, el tipo de mob y la distancia.</li>
	///   <li>Alimenta al agente con el modo y la distancia preferida.</li>
	/// </ul>
	@Override
	public void sense(MobEmployee mob, deus.brainless.agent.Agent agent) {
		if (mob == null || agent == null) {
			return;
		}

		Entity target = mob.getTarget();
		if (!GuardTargeting.canKeepTarget(mob, target)) {
			target = GuardTargeting.findNearestHostile(mob).orElse(null);
			mob.setTarget(target);
		}

		GuardWeapon weapon = weaponOf(mob);

		if (target != null) {
			double dist = target.distanceTo(mob);
			boolean hasBow = weapon == GuardWeapon.BOW;
			boolean hasArrows = hasArrows(mob);
			// Ranged solo si tiene arco Y flechas. Un creeper obliga a distancia
			// incluso sin arco (el guardia se aleja y, si tiene arco+flechas, dispara).
			boolean ranged = (hasBow && hasArrows) || GuardTargeting.isCreeper(target);
			agent.sense(GuardAgent.GUARD_MODE_INPUT, ranged ? 1.0 : 0.0);
			agent.sense(GuardAgent.GUARD_DIST_INPUT,
				GuardTargeting.preferredDistance(mob, target, weapon));
		} else {
			agent.sense(GuardAgent.GUARD_MODE_INPUT, 0.0);
			agent.sense(GuardAgent.GUARD_DIST_INPUT, GuardTargeting.NORMAL_KEEP_DISTANCE);
		}
	}

	/// El ataque ocurre a través del cerebro; el hook per-tick no hace nada extra.
	@Override
	public void work(MobEmployee mob) {}

	/// Un guardia es útil sin zona de trabajo: nunca pide una.
	@Override
	public Optional<EmployeeAlert> missingStationAlert(MobEmployee mob) {
		return Optional.empty();
	}

	/// El guardia no tiene estación propia que limpiar (el mob limpia las zonas base).
	@Override
	public void clearStations(MobEmployee mob) {}

	/// El guardia no necesita nada concreto en su zona de trabajo.
	@Override
	public String missingZoneItems(MobEmployee mob, PoscArea.Area2D workArea) {
		return null;
	}

	/// Hay trabajo mientras el guardia tenga un objetivo hostil a quien perseguir.
	@Override
	public boolean hasWorkToDo(MobEmployee mob) {
		return mob != null && GuardTargeting.canKeepTarget(mob, mob.getTarget());
	}

	/// Estado nuevo y vacío para un mob con este oficio.
	@Override
	public EmployeeJobState createState() {
		return new GuardJobState();
	}

	// ------------------------------------------------------------------ armas

	/// El arma equipada del guardia, o {@code null} si el job state no es de guardia.
	public static GuardWeapon weaponOf(MobEmployee mob) {
		if (mob == null) {
			return null;
		}
		if (mob.jobState() instanceof GuardJobState state) {
			return state.weapon();
		}
		return null;
	}

	/// El slot del inventario donde está el arma equipada, o {@code -1}.
	public static int weaponSlotOf(MobEmployee mob) {
		if (mob == null || !(mob.jobState() instanceof GuardJobState state)) {
			return -1;
		}
		return state.weaponSlot();
	}

	/// Equipa un arma al guardia, tomándola de su propio inventario.
	///
	/// {@code weapon} {@link GuardWeapon#NONE} desequipa cualquier arma.
	public static void equipWeapon(MobEmployee mob, GuardWeapon weapon, int slot) {
		if (mob == null) {
			return;
		}
		if (mob.jobState() instanceof GuardJobState state) {
			state.setWeapon(weapon, slot);
		}
	}

	/// Devuelve el ItemStack del arma equipada, o null si no hay.
	public static ItemStack equippedWeaponStack(MobEmployee mob) {
		if (mob == null) {
			return null;
		}
		GuardWeapon w = weaponOf(mob);
		if (w == GuardWeapon.NONE) {
			return null;
		}
		int slot = weaponSlotOf(mob);
		if (slot < 0 || slot >= mob.inventory.getContainerSize()) {
			return null;
		}
		return mob.inventory.getItem(slot);
	}

	/// Si un ítem es un arco.
	public static boolean isBow(Item item) {
		return item == Items.TOOL_BOW;
	}

	/// Si un ítem es una espada (cualquier herramienta con daño contra entidades).
	public static boolean isSword(ItemStack stack) {
		if (stack == null || stack.getItem() == null) {
			return false;
		}
		Item item = stack.getItem();
		if (isBow(item)) {
			return false;
		}
		return item.getDamageVsEntity(stack, null) > 0;
	}

	// ------------------------------------------------------------------ flechas

	/// Si el guardia tiene al menos una flecha en su inventario.
	public static boolean hasArrows(MobEmployee mob) {
		if (mob == null) {
			return false;
		}
		for (int i = 0; i < mob.inventory.getContainerSize(); i++) {
			ItemStack stack = mob.inventory.getItem(i);
			if (stack != null && stack.stackSize > 0 && stack.getItem() == Items.AMMO_ARROW) {
				return true;
			}
		}
		return false;
	}

	/// Consume una flecha del inventario del guardia. Devuelve true si pudo.
	public static boolean consumeArrow(MobEmployee mob) {
		if (mob == null) {
			return false;
		}
		for (int i = 0; i < mob.inventory.getContainerSize(); i++) {
			ItemStack stack = mob.inventory.getItem(i);
			if (stack != null && stack.stackSize > 0 && stack.getItem() == Items.AMMO_ARROW) {
				stack.stackSize--;
				if (stack.stackSize <= 0) {
					mob.inventory.setItem(i, null);
				}
				return true;
			}
		}
		return false;
	}
}
