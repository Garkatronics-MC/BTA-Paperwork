package deus.paperwork.ai.tasks;

import deus.brainless.agent.AgentMob;
import deus.brainless.task.StopReason;
import deus.brainless.task.Task;
import deus.brainless.task.TaskStatus;
import deus.paperwork.Paperwork;

import java.util.function.Supplier;

/// Stays put until a supplied desire drops below its threshold (i.e. until a
/// higher priority drive takes over). Runs indefinitely while the desire is high.
public class Stay implements Task {

	private static final int LOG_EVERY = 100;

	private final AgentMob mob;
	private final String name;
	private final Supplier<Double> desireSupplier;
	private final double satisfiedThreshold;

	private int logCooldown = 0;

	public Stay(AgentMob mob, String name, Supplier<Double> desireSupplier, double satisfiedThreshold) {
		this.mob = mob;
		this.name = name + "_stay";
		this.desireSupplier = desireSupplier;
		this.satisfiedThreshold = satisfiedThreshold;
	}

	@Override
	public void start() {
		logCooldown = 0;
		mob.stopMoving();
	}

	@Override
	public TaskStatus tick() {
		double desire = desireSupplier.get();
		if (desire <= satisfiedThreshold) {
			Paperwork.LOGGER.info("[{}] satisfied (desire={} threshold={}), done", name, desire, satisfiedThreshold);
			return TaskStatus.SUCCESS;
		}
		if (logCooldown-- <= 0) {
			logCooldown = LOG_EVERY;
			Paperwork.LOGGER.info("[{}] staying... desire={} threshold={}", name, desire, satisfiedThreshold);
		}
		return TaskStatus.RUNNING;
	}

	@Override
	public void stop(StopReason reason) {
		Paperwork.LOGGER.info("[{}] stop ({})", name, reason);
	}
}
