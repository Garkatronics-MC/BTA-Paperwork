package deus.paperwork.entity.employee.chef;

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
import deus.brainless.drive.Drives;
import deus.paperwork.Paperwork;
import deus.paperwork.core.ai.PaperworkDrives;
import deus.paperwork.core.ai.tasks.Goto;
import deus.paperwork.core.util.PoscArea;
import deus.paperwork.entity.employee.MobEmployee;
import deus.paperwork.entity.employee.MobEmployeeAgent;
import deus.paperwork.entity.employee.model.EmployeeStations;
import deus.paperwork.entity.employee.model.EmployeeStats;
import deus.paperwork.entity.employee.model.EmployeeTraits;

import java.util.Optional;

/// The farmer brain: the employee brain extended with a work drive that
/// farms. Built by extending the employee's template with
/// {@link Temperament#extend()} so plain employees never see the work drive.
///
/// The drive and the planner are the ones the farmer has always had: this is
/// a move, not a rebalance.
public final class ChefAgent {

	private static final Temperament TEMPERAMENT = buildTemperament();
	private static final GoapPlanner PLANNER = buildPlanner();
	private static final DriveGoals GOALS = buildGoals();

	private ChefAgent() {}

	/// Builds the farmer brain wired to {@code mob}. Same contract as
	/// {@link MobEmployeeAgent#build}: constants written once, stats re-sensed
	/// every tick by the owner.
	public static Agent build(MobEmployee mob, EmployeeTraits traits, EmployeeStats stats, EmployeeStations stations) {
		Psyche psyche = new Psyche(TEMPERAMENT);
		GoapArbiter arbiter = new GoapArbiter(
			new HighestPriorityProposalSelector(),
			new ArbiterConfig(0.0, MobEmployeeAgent.WANDER)
		);
		TaskFactory<GoapAction> tasks = action -> switch (action.id()) {
			case "goto_kitchen" -> Goto.fromArea(mob, stations.workPlace(), "kitchen", 2.5, 0);
			case "kitchen"      -> new CookTask(mob);
			default          -> MobEmployeeAgent.employeeTask(action, mob, stations, stats);
		};
		// Diagnóstico: si el chef no tiene área de trabajo asignada, nunca
		// podrá cocinar. Loguearlo una vez para ayudar a depurar.
		Optional<? extends PoscArea> workPlaceOpt = stations.workPlace().get();
		if (workPlaceOpt.isEmpty()) {
			Paperwork.LOGGER.warn("[ChefAgent] mob @{} has no workArea assigned — it will not cook until an area is set with the WorkAreaStick", System.identityHashCode(mob));
		}

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

		/// El chef prioriza cocinar sobre comer: cuando su stat de trabajo está
		/// bajo (necesita trabajar para recargarlo), el deseo de comer se reduce
		/// para que el drive de trabajo pueda ejecutarse primero. Si no hay
		/// comida en el área de comida, el chef no se queda atorado intentando
		/// comer; pasa a cocinar.
		t.layer("chef_work_priority", layer -> {
			layer.mix("chef_eat_mod")
				.sub("work", 0.9)
				.sigmoid(7)
				.onHigh(Drives.HUNGER);

			layer.mix("chef_work_mod")
				.add("work", 1.2)
				.mul("work_available", 1.0)
				.sigmoid(5)
				.onHigh(PaperworkDrives.WORK);
		});

		return t;
	}

	private static GoapPlanner buildPlanner() {
		return MobEmployeeAgent.extendEmployeePlanner()
			// farm: walk to the farm area, then work it (the Farm task re-checks
			// the real world itself and succeeds once the area has nothing left).
			.addAction(new GoapAction("goto_kitchen",
				s -> s.get("cooked") < 1f && s.get("atKitchen") < 1f,
				s -> s.set("atKitchen", 1f),
				s -> 1.0))
			.addAction(new GoapAction("kitchen",
				s -> s.get("atKitchen") >= 1f && s.get("cooked") < 1f,
				s -> s.set("cooked", 1f),
				s -> 2.0));
	}

	private static DriveGoals buildGoals() {
		return (drive, world) -> {
			Optional<Goal> goal = MobEmployeeAgent.employeeGoals().goalFor(drive, world);
			if (goal.isPresent()) return goal;

			if (drive == PaperworkDrives.WORK) {
				goal = Optional.of(Goal.of(s -> s.get("cooked") >= 1f, world));
			}
			return goal;
		};
	}
}
