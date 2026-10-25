package deus.paperwork.entity.employee.guard;

import deus.brainless.Brainless;
import deus.brainless.agent.AgentMob;
import deus.brainless.task.StopReason;
import deus.brainless.task.Task;
import deus.brainless.task.TaskStatus;
import deus.brainless.utils.Timer;
import deus.paperwork.entity.employee.MobEmployee;
import deus.paperwork.entity.employee.guard.GuardWeapon;
import net.minecraft.core.entity.Entity;

/**
 * Camina hacia el objetivo actual. El alcance de llegada depende del modo de
 * combate del guardia: con arco se detiene más lejos, con espada o sin arma se
 * acerca hasta el alcance de golpe.
 */
public class GuardApproach implements Task {

	private static final long TIMEOUT_TICKS = 160;

	private final AgentMob mob;
	private final Timer timeout = Timer.fromTicks(TIMEOUT_TICKS);

	public GuardApproach(AgentMob mob) {
		this.mob = mob;
	}

	@Override
	public void start() {
		timeout.reset();
		Entity target = mob.getTarget();
		if (target == null || !target.isAlive()) {
			mob.stopMoving();
			return;
		}
		mob.navigateTo(target.x, target.y, target.z);
	}

	@Override
	public TaskStatus tick() {
		timeout.tick();

		Entity target = mob.getTarget();
		if (target == null || !target.isAlive()) {
			mob.stopMoving();
			return TaskStatus.SUCCESS;
		}

		// Re-apunta cada tick mientras el objetivo se mueve.
		mob.navigateTo(target.x, target.y, target.z);

		double distance = mob.distanceTo(target);
		if (distance <= reach()) {
			mob.stopMoving();
			Brainless.LOGGER.debug("[guard] approach reached target at {}", distance);
			return TaskStatus.SUCCESS;
		}

		if (timeout.hasElapsed()) {
			mob.stopMoving();
			Brainless.LOGGER.debug("[guard] approach gave up after {}", timeout);
			return TaskStatus.FAILURE;
		}
		return TaskStatus.RUNNING;
	}

	@Override
	public void stop(StopReason reason) {
		mob.stopMoving();
	}

	/// Alcance al que el guardia considera "llegado" al objetivo.
	private double reach() {
		Entity target = mob.getTarget();
		if (target == null) {
			return GuardTargeting.FIST_REACH;
		}
		GuardWeapon weapon = weaponOf(mob);
		if (mob instanceof MobEmployee employee) {
			return GuardTargeting.preferredDistance(employee, target, weapon);
		}
		return GuardTargeting.NORMAL_KEEP_DISTANCE;
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
