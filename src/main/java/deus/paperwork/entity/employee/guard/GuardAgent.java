package deus.paperwork.entity.employee.guard;

import deus.brainless.agent.Agent;
import deus.brainless.agent.DriveGoals;
import deus.brainless.agent.Goal;
import deus.brainless.goap.GoapAction;
import deus.brainless.goap.GoapPlanner;
import deus.brainless.goap.arbitration.ArbiterConfig;
import deus.brainless.goap.arbitration.GoapArbiter;
import deus.brainless.goap.arbitration.HighestPriorityProposalSelector;
import deus.brainless.limbic.Psyche;
import deus.brainless.limbic.Temperament;
import deus.brainless.task.TaskFactory;
import deus.brainless.utils.Timer;
import deus.paperwork.core.ai.PaperworkDrives;
import deus.paperwork.entity.employee.MobEmployee;
import deus.paperwork.entity.employee.MobEmployeeAgent;
import deus.paperwork.entity.employee.model.EmployeeStats;
import deus.paperwork.entity.employee.model.EmployeeStations;
import deus.paperwork.entity.employee.model.EmployeeTraits;

import java.util.Optional;

/// The guard brain: the employee brain extended with a drive to fight the
/// hostile mobs around it.
///
/// The fight itself reuses the actions every employee already has
/// ({@code approach}, {@code strike}, {@code retreat}): the guard only adds
/// the drive that makes those actions run when there is a monster nearby.
/// The target is picked by the job (see {@link GuardJob#sense}) and lives in
/// the mob, which is what the Brainless fight tasks read.
public final class GuardAgent {

	/// Input fed by the job every tick: 1 when the guard has a hostile target,
	/// 0 when there is nothing to fight.
	static final String GUARD_THREAT_INPUT = "guard_threat";

	private static final Temperament TEMPERAMENT = buildTemperament();
	private static final GoapPlanner PLANNER = MobEmployeeAgent.extendEmployeePlanner();
	private static final DriveGoals GOALS = buildGoals();

	private GuardAgent() {}

	/// Builds the guard brain wired to {@code mob}. Same contract as
	/// {@link MobEmployeeAgent#build}: constants written once, stats and
	/// threat re-sensed every tick by the owner.
	public static Agent build(MobEmployee mob, EmployeeTraits traits, EmployeeStats stats, EmployeeStations stations) {
		Psyche psyche = new Psyche(TEMPERAMENT);
		GoapArbiter arbiter = new GoapArbiter(
			new HighestPriorityProposalSelector(),
			new ArbiterConfig(0.0, MobEmployeeAgent.WANDER)
		);
		TaskFactory<GoapAction> tasks = action -> MobEmployeeAgent.employeeTask(action, mob, stations, stats);
		Agent agent = new Agent(
			psyche,
			arbiter,
			PLANNER,
			GOALS,
			tasks,
			mob.controller(),
			Timer.fromTicks(3)
		);
		traits.set(agent);
		stats.set(agent);
		return agent;
	}

	private static Temperament buildTemperament() {
		Temperament t = MobEmployeeAgent.extendEmployeeTemplate();

		// The threat is not a stat: it rises and falls with the monsters around
		// the guard, so it is declared and fed by the job itself.
		t.inputs(in -> in.declare(GUARD_THREAT_INPUT, 0.0));

		t.layer("guard_duty", layer -> {
			// eagerness to fight: no monster, no drive. A brave guard still
			// gets a small push so it does not hesitate before engaging.
			layer.mix("guard_duty")
				.add(GUARD_THREAT_INPUT, 1.0)
				.add("trait_brave", 0.2)
				.mul(GUARD_THREAT_INPUT, 1.0)
				.sigmoid(6)
				.onHigh(PaperworkDrives.GUARD);
		});

		return t;
	}

	private static DriveGoals buildGoals() {
		return (drive, world) -> {
			Optional<Goal> goal = MobEmployeeAgent.employeeGoals().goalFor(drive, world);
			if (goal.isPresent()) return goal;

			if (drive == PaperworkDrives.GUARD) {
				// The same goal as anger: hit the target at least once. The
				// guard drive only decides WHEN to fight, not how.
				goal = Optional.of(Goal.of(s -> s.get("attacked") >= 1f, world));
			}
			return goal;
		};
	}
}
