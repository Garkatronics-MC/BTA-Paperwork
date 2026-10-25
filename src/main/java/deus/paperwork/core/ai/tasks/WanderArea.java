package deus.paperwork.core.ai.tasks;

import deus.brainless.agent.AgentMob;
import deus.brainless.task.StopReason;
import deus.brainless.task.Task;
import deus.brainless.task.TaskStatus;
import deus.brainless.utils.Timer;
import deus.paperwork.core.util.PoscArea;
import net.minecraft.core.world.pos.TilePos;
import net.minecraft.core.world.pos.TilePosc;

import java.util.Optional;
import java.util.function.Supplier;

/// Wanders around the workplace so an idle employee stays near its job instead
/// of drifting away. When the area is gone it behaves like the plain wander.
///
/// It is interruptible (the default) and also yields as soon as the job reports
/// pending work: returning SUCCESS lets the agent re-plan and pick the work
/// drive instead of finishing the current stroll.
public class WanderArea implements Task {

	private static final long DURATION_TICKS = 60;
	private static final double MAX_RADIUS = 8.0;
	private static final double MIN_RADIUS = 1.5;
	private static final int PICK_ATTEMPTS = 8;
	/// How often the "is there work?" check runs while strolling.
	private static final long WORK_CHECK_INTERVAL = 20;

	private final AgentMob mob;
	private final Supplier<Optional<? extends PoscArea>> area;
	private final Supplier<Boolean> workAvailable;
	private final Timer duration = Timer.fromTicks(DURATION_TICKS);
	private final Timer workCheck = Timer.fromTicks(WORK_CHECK_INTERVAL);

	private double targetX;
	private double targetZ;

	public WanderArea(AgentMob mob,
	                  Supplier<Optional<? extends PoscArea>> area,
	                  Supplier<Boolean> workAvailable) {
		this.mob = mob;
		this.area = area;
		this.workAvailable = workAvailable;
	}

	@Override
	public void start() {
		duration.reset();
		workCheck.reset();
		pickTarget();
	}

	@Override
	public TaskStatus tick() {
		duration.tick();
		workCheck.tick();

		if (workCheck.hasElapsed()) {
			workCheck.reset();
			if (Boolean.TRUE.equals(workAvailable.get())) {
				mob.stopMoving();
				return TaskStatus.SUCCESS;
			}
		}

		if (duration.hasElapsed()) {
			mob.stopMoving();
			return TaskStatus.SUCCESS;
		}

		mob.navigateTo(targetX, targetZ);
		return TaskStatus.RUNNING;
	}

	/// Picks a random point within the workplace, close to where the employee
	/// already stands so it strolls around instead of crossing the whole area.
	private void pickTarget() {
		Optional<? extends PoscArea> opt = area.get();
		if (opt != null && opt.isPresent()) {
			PoscArea a = opt.get();
			int y = (int) Math.floor(mob.y);
			for (int attempt = 0; attempt < PICK_ATTEMPTS; attempt++) {
				double angle = mob.random().nextDouble() * Math.PI * 2.0;
				double distance = MIN_RADIUS + mob.random().nextDouble() * (MAX_RADIUS - MIN_RADIUS);
				double x = mob.x + Math.cos(angle) * distance;
				double z = mob.z + Math.sin(angle) * distance;
				if (a.contains(new TilePos((int) Math.floor(x), y, (int) Math.floor(z)))) {
					targetX = x;
					targetZ = z;
					return;
				}
			}
			// No random point landed inside (tiny area): aim at its centre.
			TilePosc centre = a.center();
			targetX = centre.x() + 0.5;
			targetZ = centre.z() + 0.5;
			return;
		}

		// No workplace: the plain wander, a short stroll in any direction.
		double angle = mob.random().nextDouble() * Math.PI * 2.0;
		targetX = mob.x + Math.cos(angle) * MAX_RADIUS;
		targetZ = mob.z + Math.sin(angle) * MAX_RADIUS;
	}

	@Override
	public void stop(StopReason reason) {
		mob.stopMoving();
	}
}
