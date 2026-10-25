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

/// El cerebro del guardia: el cerebro del empleado extendido con dos drives
/// de pelea — {@code guard_melee} y {@code guard_ranged} — que se seleccionan
/// según la distancia al objetivo, el tipo de mob (creeper = distancia) y el
/// arma equipada (arco o espada).
///
/// Las acciones de pelea se reusan del empleado ({@code approach}, {@code
/// strike}) pero con reach distinto según el modo, por medio de
/// {@link GuardApproach} y {@link GuardAttack}.
public final class GuardAgent {

	/// Inputs que el job escribe en el agente cada tick.
	static final String GUARD_MODE_INPUT   = "guard_mode";   // 0 = melee, 1 = ranged
	static final String GUARD_DIST_INPUT   = "guard_dist";   // distancia preferida al objetivo

	private static final Temperament TEMPERAMENT = buildTemperament();
	private static final GoapPlanner PLANNER = buildPlanner();
	private static final DriveGoals GOALS = buildGoals();

	private GuardAgent() {}

	/// Construye el cerebro del guardia. Los rasgos y las estadísticas se
	/// escriben una vez; el modo y la distancia se re-sensan cada tick desde
	/// el job.
	public static Agent build(MobEmployee mob, EmployeeTraits traits, EmployeeStats stats, EmployeeStations stations) {
		Psyche psyche = new Psyche(TEMPERAMENT);
		GoapArbiter arbiter = new GoapArbiter(
			new HighestPriorityProposalSelector(),
			new ArbiterConfig(0.0, MobEmployeeAgent.WANDER)
		);
		TaskFactory<GoapAction> tasks = action -> guardTask(action, mob);
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

	/// Mapea una acción GOAP a su tarea. El guardia reusa las tareas de
	/// empleado pero con reach y comportamiento adaptados a su arma.
	private static deus.brainless.task.Task guardTask(GoapAction action, MobEmployee mob) {
		return switch (action.id()) {
			case "approach" -> new GuardApproach(mob);
			case "strike"   -> new GuardAttack(mob);
			case "retreat"  -> new deus.brainless.task.behaviour.Flee(mob);
			case "recover"  -> new deus.brainless.task.behaviour.Recover(mob, 60);
			case "wander"   -> new deus.brainless.task.behaviour.Wander(mob);
			default         -> deus.brainless.task.Tasks.waitFor(1);
		};
	}

	private static Temperament buildTemperament() {
		Temperament t = MobEmployeeAgent.extendEmployeeTemplate();

		t.inputs(in -> {
			in.declare(GUARD_MODE_INPUT, 0.0);   // 0 = melee, 1 = ranged
			in.declare(GUARD_DIST_INPUT, 3.0);   // distancia preferida al objetivo
		});

		// Drive de pelea cuerpo a cuerpo: se activa cuando no hay amenaza de
		// rango o el objetivo está muy cerca.
		t.layer("guard_melee", layer -> {
			layer.mix("guard_melee")
				.bias(1.0)                    // 1.0 cuando no hay modo
				.mul(GUARD_MODE_INPUT, -1.0)  // resta el modo (0=melee, 1=ranged)
				.sigmoid(6)
				.onHigh(PaperworkDrives.GUARD_MELEE);
		});

		// Drive de pelea a distancia: se activa cuando hay arco y el objetivo
		// está lejos (o es un creeper, que obliga a distancia).
		t.layer("guard_ranged", layer -> {
			layer.mix("guard_ranged")
				.add(GUARD_MODE_INPUT, 1.0)   // 1 = ranged activo
				.sigmoid(6)
				.onHigh(PaperworkDrives.GUARD_RANGED);
		});

		return t;
	}

	private static GoapPlanner buildPlanner() {
		return new GoapPlanner()
			.addAction(new GoapAction("approach",
				s -> s.get("targetDist") > 0.05f,
				s -> s.set("targetDist", Math.max(0f, s.get("targetDist") - 0.4f)),
				s -> 1.0))
			.addAction(new GoapAction("strike",
				s -> s.get("targetDist") <= 0.05f && s.get("attacked") < 1f,
				s -> s.set("attacked", 1f),
				s -> 2.0))
			.addAction(new GoapAction("retreat",
				s -> s.get("fled") < 1f,
				s -> s.set("fled", 1f),
				s -> 1.0))
			.addAction(new GoapAction("recover",
				s -> s.get("fled") >= 1f && s.get("safe") < 1f,
				s -> s.set("safe", 1f),
				s -> 3.0))
			.addAction(new GoapAction("wander",
				s -> s.get("wandered") < 1f,
				s -> s.set("wandered", 1f),
				s -> 1.0));
	}

	private static DriveGoals buildGoals() {
		return (drive, world) -> {
			Optional<Goal> goal = MobEmployeeAgent.employeeGoals().goalFor(drive, world);
			if (goal.isPresent()) return goal;

			if (drive == PaperworkDrives.GUARD_MELEE || drive == PaperworkDrives.GUARD_RANGED) {
				goal = Optional.of(Goal.of(s -> s.get("attacked") >= 1f, world));
			}
			return goal;
		};
	}
}
