package deus.paperwork.ai.tasks;

import deus.brainless.agent.AgentMob;
import deus.brainless.task.StopReason;
import deus.brainless.task.Task;
import deus.brainless.task.TaskStatus;
import deus.brainless.utils.Timer;
import deus.paperwork.Paperwork;
import deus.paperwork.util.PoscArea;
import net.minecraft.core.world.pos.TilePos;
import net.minecraft.core.world.pos.TilePosc;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Walks to a fixed tile target — a station, not a living entity (for those,
 * Brainless's own {@code Approach} exists). Behaves exactly like Approach
 * except the destination:
 * <ul>
 *   <li>Re-aims every tick. Cheap: the navigator ignores a destination that
 *       barely drifted, so this does not throw the path away every tick.</li>
 *   <li>Fails on timeout so the agent re-plans.</li>
 * </ul>
 */
public class Goto implements Task {

	private static final long TIMEOUT_TICKS = 160;

	private final AgentMob mob;
	private final String name;
	private final Supplier<Optional<TilePosc>> targetSupplier;
	private final double interactionRadius;
	private final Timer timeout = Timer.fromTicks(TIMEOUT_TICKS);

	private boolean arrived = false;

	public Goto(AgentMob mob, Supplier<Optional<TilePosc>> targetSupplier, String name, double interactionRadius) {
		this.mob = mob;
		this.targetSupplier = targetSupplier;
		this.name = name + "_goto";
		this.interactionRadius = interactionRadius;
	}

	public static Goto fromArea(
		AgentMob mob, Supplier<Optional<? extends PoscArea>> areaSupplier, String name, double radius, int py) {
		return new Goto(
			mob,
			() -> {
				Optional<? extends PoscArea> opt = areaSupplier.get();
				if (opt == null) return Optional.empty();
				return opt.flatMap(a -> Optional.of(new TilePos(a.center().x(), a.center().y() + py, a.center().z())));
			},
			name, radius
		);
	}

	@Override
	public void start() {
		arrived = false;
		timeout.reset();
	}

	@Override
	public TaskStatus tick() {
		if (arrived) return TaskStatus.SUCCESS;
		timeout.tick();

		Optional<TilePosc> targetOpt = targetSupplier.get();
		if (targetOpt.isEmpty()) {
			mob.stopMoving();
			Paperwork.LOGGER.info("[{}] target empty, failing", name);
			return TaskStatus.FAILURE;
		}
		TilePosc target = targetOpt.get();

		if (near(target)) {
			Paperwork.LOGGER.info("[{}] arrived at {}", name, target);
			mob.stopMoving();
			arrived = true;
			return TaskStatus.SUCCESS;
		}

		// Re-aim as the destination drifts or the path fails. Cheap: the
		// navigator ignores a destination that barely changed, so re-issuing
		// it every tick doesn't discard the path they were already taking.
		mob.navigateTo(target.x(), target.y(), target.z());

		if (timeout.hasElapsed()) {
			mob.stopMoving();
			Paperwork.LOGGER.info("[{}] gave up after {}", name, timeout);
			return TaskStatus.FAILURE;
		}
		return TaskStatus.RUNNING;
	}

	@Override
	public void stop(StopReason reason) {
		Paperwork.LOGGER.info("[{}] stop ({}), stop walking", name, reason);
		mob.stopMoving();
	}

	/// Same 2D distance rule the old MobEmployee.near used: ignores height.
	private boolean near(TilePosc target) {
		double dx = target.x() + 0.5 - mob.x;
		double dz = target.z() + 0.5 - mob.z;
		return Math.sqrt(dx * dx + dz * dz) <= interactionRadius;
	}
}
