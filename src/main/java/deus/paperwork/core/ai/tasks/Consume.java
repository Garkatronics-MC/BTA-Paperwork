package deus.paperwork.core.ai.tasks;

import deus.brainless.agent.AgentMob;
import deus.brainless.task.StopReason;
import deus.brainless.task.Task;
import deus.brainless.task.TaskStatus;
import deus.paperwork.Paperwork;
import net.minecraft.core.world.pos.TilePosc;

import java.util.Optional;
import java.util.function.Supplier;

/// Consume items (food): makes sure the employee stands at a stocked
/// chest — see {@link Food}, the seam that supplies chest lookup and the
/// actual bite — then eats from it. Succeeds once fed; fails when there is
/// no food source or no chest worth walking to.
public class Consume implements Task {

	private static final double EAT_REACH = 2.5;

	private final AgentMob mob;
	private final Supplier<Optional<Food>> source;

	private boolean ate = false;

	public Consume(AgentMob mob, Supplier<Optional<Food>> source) {
		this.mob = mob;
		this.source = source;
	}

	@Override
	public void start() {
		ate = false;
	}

	@Override
	public TaskStatus tick() {
		if (ate) return TaskStatus.SUCCESS;

		return source.get().map(food -> {
			Optional<TilePosc> chest = food.nearestChest(mob.x, mob.z);
			if (chest.isEmpty()) {
				Paperwork.LOGGER.info("[Consume] No chest worth walking to");
				return TaskStatus.FAILURE;
			}
			TilePosc spot = chest.get();

			if (!near(spot)) {
				Paperwork.LOGGER.info("[Consume] Moving to chest at {}", spot);
				mob.navigateTo(spot.x(), spot.y(), spot.z());
				return TaskStatus.RUNNING;
			}

			boolean success = food.eatFrom(spot);
			Paperwork.LOGGER.info("[Consume] Eat attempt at {} -> {}", spot, success ? "OK" : "No food found");
			if (success) {
				mob.stopMoving();
				ate = true;
				return TaskStatus.SUCCESS;
			}
			return TaskStatus.FAILURE;

		}).orElseGet(() -> {
			Paperwork.LOGGER.info("[Consume] no Food source supplied");
			return TaskStatus.FAILURE;
		});
	}

	@Override
	public boolean interruptible() {
		// Interruption mid-meal would leave the mob shifting restlessly
		return !ate;
	}

	@Override
	public void stop(StopReason reason) {
		Paperwork.LOGGER.info("[Consume] stop ({}) — resetting", reason);
		ate = false;
		mob.stopMoving();
	}

	/// 2D distance to a TilePosc, matching how stations are measured elsewhere.
	private boolean near(TilePosc spot) {
		double dx = spot.x() + 0.5 - mob.x;
		double dz = spot.z() + 0.5 - mob.z;
		return Math.sqrt(dx * dx + dz * dz) <= EAT_REACH;
	}
}
