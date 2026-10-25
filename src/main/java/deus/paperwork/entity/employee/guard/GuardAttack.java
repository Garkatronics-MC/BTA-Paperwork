package deus.paperwork.entity.employee.guard;

import deus.brainless.Brainless;
import deus.brainless.agent.AgentMob;
import deus.brainless.task.StopReason;
import deus.brainless.task.Task;
import deus.brainless.task.TaskStatus;
import deus.paperwork.entity.employee.MobEmployee;
import deus.paperwork.entity.employee.guard.GuardWeapon;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.util.helper.DamageType;

/**
 * Ataca al objetivo actual. Respetando el modo de combate del guardia:
 * <ul>
 *   <li>Con arco ({@link GuardWeapon#BOW}) y objetivo dentro del alcance de
 *       disparo, aplica daño a distancia sin necesidad de tocarlo.</li>
 *   <li>Con espada o sin arma, golpea cuerpo a cuerpo cuando está en alcance.</li>
 * </ul>
 *
 * Si es un creeper y el guardia está demasiado cerca, no golpea: deja que el
 * approach (o retreat) gestione la distancia.
 */
public class GuardAttack implements Task {

	private static final float REACH = 2.5f;
	private static final float GIVE_UP_BEYOND = 6.0f;

	private final AgentMob mob;

	public GuardAttack(AgentMob mob) {
		this.mob = mob;
	}

	@Override
	public void start() {}

	@Override
	public TaskStatus tick() {
		Entity target = mob.getTarget();
		if (target == null || !target.isAlive()) return TaskStatus.SUCCESS;

		mob.face(target);

		float distance = mob.distanceTo(target);
		if (distance > GIVE_UP_BEYOND) {
			Brainless.LOGGER.debug("[guard] strike lost target dist={}", distance);
			return TaskStatus.FAILURE;
		}

		GuardWeapon weapon = weaponOf(mob);

		// Con arco, flechas y dentro del alcance de disparo: ataque a distancia.
		if (weapon == GuardWeapon.BOW && distance <= GuardTargeting.BOW_REACH) {
			if (!GuardJob.hasArrows((MobEmployee) mob)) {
				// Sin flechas: no puede disparar, deja que el approach ajuste la
				// distancia o que se acerque a pelear cuerpo a cuerpo si tiene espada.
				return TaskStatus.RUNNING;
			}
			shoot((MobEmployee) mob, target);
			Brainless.LOGGER.debug("[guard] bow shot at {} dist={}", target.getClass().getSimpleName(), distance);
			return TaskStatus.SUCCESS;
		}

		// Cuerpo a cuerpo: solo golpea si está dentro del alcance adecuado y
		// no está demasiado cerca de un creeper (que obligaría a alejarse).
		if (distance <= REACH && !GuardTargeting.tooCloseToCreeper(target, mob)) {
			mob.attack(target, distance);
			Brainless.LOGGER.debug("[guard] melee hit {} dist={}", target.getClass().getSimpleName(), distance);
			return TaskStatus.SUCCESS;
		}

		return TaskStatus.RUNNING;
	}

	@Override
	public void stop(StopReason reason) {}

	/// Dispara el arco: aplica daño directo al objetivo (BTA 8.0.1 no tiene
	/// entidad de flecha separada, por tanto el impacto se resuelve como daño
	/// directo al alcance del arco).
	private static void shoot(MobEmployee mob, Entity target) {
		// Consume una flecha del inventario antes de disparar.
		GuardJob.consumeArrow(mob);
		int damage = bowDamage(mob);
		target.hurt(mob, damage, DamageType.COMBAT);
	}

	private static int bowDamage(MobEmployee employee) {
		GuardWeapon w = GuardJob.weaponOf(employee);
		if (w == GuardWeapon.BOW && employee.jobState() instanceof GuardJobState state) {
			int slot = state.weaponSlot();
			if (slot >= 0) {
				ItemStack stack = employee.inventory.getItem(slot);
				if (stack != null) {
					// Daño base del arco + un poco.
					return 5;
				}
			}
		}
		return 4;
	}

	private static GuardWeapon weaponOf(AgentMob mob) {
		if (mob instanceof MobEmployee employee) {
			GuardWeapon w = GuardJob.weaponOf(employee);
			if (w != null) {
				return w;
			}
		}
		return GuardWeapon.NONE;
	}
}
