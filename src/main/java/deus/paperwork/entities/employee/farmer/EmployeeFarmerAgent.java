package deus.paperwork.entities.employee.farmer;

import deus.brainless.agent.Agent;
import deus.brainless.agent.DriveGoals;
import deus.brainless.agent.Goal;
import deus.brainless.drive.Drive;
import deus.brainless.goap.GoapAction;
import deus.brainless.goap.GoapPlanner;
import deus.brainless.goap.arbitration.ArbiterConfig;
import deus.brainless.goap.arbitration.GoapArbiter;
import deus.brainless.goap.arbitration.HighestPriorityProposalSelector;
import deus.brainless.limbic.Psyche;
import deus.brainless.limbic.Temperament;
import deus.brainless.task.TaskFactory;
import deus.brainless.utils.Timer;
import deus.paperwork.ai.tasks.Farm;
import deus.paperwork.ai.tasks.Goto;
import deus.paperwork.entities.employee.MobEmployeeAgent;
import deus.paperwork.entities.employee.model.EmployeeStats;
import deus.paperwork.entities.employee.model.EmployeeStations;
import deus.paperwork.entities.employee.model.EmployeeTraits;

import java.util.Optional;

/// The employee brain, extended into a farmer: same limbs, senses and
/// safety behaviour, plus a work drive that farms. Built by extending the
/// employee's template with {@link Temperament#extend()} so plain employees
/// never see the work drive.
public final class EmployeeFarmerAgent {

	public static final Drive WORK = Drive.of("work");

	private static final Temperament TEMPERAMENT = buildTemperament();
	private static final GoapPlanner PLANNER = buildPlanner();
	private static final DriveGoals GOALS = buildGoals();

	private EmployeeFarmerAgent() {}

	/// Builds the farmer brain wired to {@code mob}. Same contract as
	/// {@link MobEmployeeAgent#build}: constants written once, stats re-sensed
	/// every tick by the owner.
	public static Agent build(MobEmployeeFarmer mob, EmployeeTraits traits, EmployeeStats stats, EmployeeStations stations) {
		Psyche psyche = new Psyche(TEMPERAMENT);
		GoapArbiter arbiter = new GoapArbiter(
			new HighestPriorityProposalSelector(),
			new ArbiterConfig(0.0, MobEmployeeAgent.WANDER)
		);
		TaskFactory<GoapAction> tasks = action -> switch (action.id()) {
			case "goto_farm" -> Goto.fromArea(mob, stations.workPlace(), "farm", 2.5, 0);
			case "farm"      -> new Farm(mob);
			default          -> MobEmployeeAgent.employeeTask(action, mob, stations, stats);
		};
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

		// --- the work drive (the old EmployeeAI.desire_work, ported) ---
		// eagerness to work, fed by the `work` deficit and hardworking nature,
		// damped by lower needs: a hungry, tired or scared farmer won't farm.
		t.layer("drives", layer ->
			layer.mix("desire_work")
				.add("work", 1.0)
				.add("trait_hardworking", 0.6)
				.add("health", 0.2)
				.sub("hunger", 0.9)
				.sub("fatigue", 0.4)
				.sub("danger", 0.7)
				.sub("social", 0.2)
				.sigmoid(5)
				.onHigh(WORK));

		return t;
	}

	private static GoapPlanner buildPlanner() {
		return MobEmployeeAgent.extendEmployeePlanner()
			// farm: walk to the farm area, then work it (the Farm task re-checks
			// the real world itself and succeeds once the area has nothing left).
			.addAction(new GoapAction("goto_farm",
				s -> s.get("farmed") < 1f && s.get("atFarm") < 1f,
				s -> s.set("atFarm", 1f),
				s -> 1.0))
			.addAction(new GoapAction("farm",
				s -> s.get("atFarm") >= 1f && s.get("farmed") < 1f,
				s -> s.set("farmed", 1f),
				s -> 2.0));
	}

	private static DriveGoals buildGoals() {
		return (drive, world) -> {
			Optional<Goal> goal = MobEmployeeAgent.employeeGoals().goalFor(drive, world);
			if (goal.isPresent()) return goal;

			if (drive == WORK) {
				goal = Optional.of(Goal.of(s -> s.get("farmed") >= 1f, world));
			}
			return goal;
		};
	}
}
