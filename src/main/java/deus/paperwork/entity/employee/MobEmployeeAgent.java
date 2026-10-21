package deus.paperwork.entity.employee;

import deus.brainless.agent.Agent;
import deus.brainless.agent.AgentMob;
import deus.brainless.agent.DriveGoals;
import deus.brainless.agent.Goal;
import deus.brainless.drive.Drive;
import deus.brainless.drive.Drives;
import deus.brainless.task.behaviour.Attack;
import deus.brainless.task.behaviour.Flee;
import deus.brainless.task.behaviour.Approach;
import deus.brainless.task.behaviour.Recover;
import deus.brainless.task.behaviour.Wander;
import deus.brainless.goap.GoapAction;
import deus.brainless.goap.GoapPlanner;
import deus.brainless.goap.arbitration.ArbiterConfig;
import deus.brainless.goap.arbitration.GoapArbiter;
import deus.brainless.goap.arbitration.HighestPriorityProposalSelector;
import deus.brainless.limbic.Psyche;
import deus.brainless.limbic.Temperament;
import deus.brainless.task.Task;
import deus.brainless.task.TaskFactory;
import deus.brainless.task.Tasks;
import deus.brainless.utils.Timer;
import deus.paperwork.core.ai.tasks.Consume;
import deus.paperwork.core.ai.tasks.Goto;
import deus.paperwork.core.ai.tasks.Stay;
import deus.paperwork.entity.employee.model.EmployeeStats;
import deus.paperwork.entity.employee.model.EmployeeStations;
import deus.paperwork.entity.employee.model.EmployeeTraits;

import java.util.Optional;

public final class MobEmployeeAgent {

	public static final Drive WANDER = Drive.of("wander");
	private static final Temperament TEMPERAMENT = buildTemperament();
	private static final GoapPlanner PLANNER = buildPlanner();
	private static final DriveGoals GOALS = buildGoals();

	private MobEmployeeAgent() {}

	/// Builds the brain wired to {@code mob}. {@code traits} are written once
	/// (they are constants); {@code stats} get their first feed here and must
	/// be re-sensed by the owner each tick (see MobEmployee.sense).
	public static Agent build(AgentMob mob, EmployeeTraits traits, EmployeeStats stats, EmployeeStations stations) {
		Psyche psyche = new Psyche(TEMPERAMENT);
		GoapArbiter arbiter = new GoapArbiter(
			new HighestPriorityProposalSelector(),
			new ArbiterConfig(0.0, WANDER)
		);
		TaskFactory<GoapAction> tasks = action -> employeeTask(action, mob, stations, stats);
		Agent agent = new Agent(
			psyche,
			arbiter,
			PLANNER,
			GOALS,
			tasks,
			mob.controller(),
			Timer.fromTicks(3)
		);
		traits.set(agent);   // constant personality inputs
		stats.set(agent);    // first feed; living inputs keep coming each tick
		return agent;
	}

	/// Task mapping for the plain employee life. Specialised agents (see
	/// EmployeeFarmerAgent) extend the brain with more actions but delegate
	/// every unknown action id back here.
	public static Task employeeTask(GoapAction action, AgentMob mob, EmployeeStations stations, EmployeeStats stats) {
		return switch (action.id()) {
			// fight behaviour (Brainless built-ins, all entity-targeted)
			case "approach" -> new Approach(mob);
			case "strike"   -> new Attack(mob);
			case "retreat"  -> new Flee(mob);
			case "recover"  -> new Recover(mob, 60);
			case "wander"   -> new Wander(mob);

			// employee life (station-targeted tasks rewritten from the old jobs)
			case "goto_food" -> Goto.fromArea(mob, stations.foodPlace(), "food", 2.5, 0);
			case "eat"       -> new Consume(mob, stations.foodSource());
			case "goto_rest" -> Goto.fromArea(mob, stations.restPlace(), "rest", 2.0, 0);
			case "rest"      -> new Stay(mob, "rest", () -> stats.fatigue, 0.2);
			default          -> Tasks.waitFor(1);
		};
	}

	/// Fresh deep copy of the employee temperament, ready to be extended with
	/// extra layers/drives (see EmployeeFarmerAgent). Extending the shared
	/// instance in place would leak the extra drive into plain employees.
	public static Temperament extendEmployeeTemplate() {
		return TEMPERAMENT.extend();
	}

	/// The employee action set as a copy: specialization adds actions without
	/// touching the employee's planner.
	public static GoapPlanner extendEmployeePlanner() {
		return new GoapPlanner(PLANNER.actions());
	}

	/// The employee goal mapping. Specialised agents delegate to it and layer
	/// their extra drives on top.
	public static DriveGoals employeeGoals() {
		return GOALS;
	}

	static Temperament buildTemperament() {
		Temperament t = new Temperament();
		t.inputs(in -> {
			in.declare("targetDist", 1.0);   // health comes from EmployeeStats.declare below
			EmployeeTraits.declare(in);   // trait_social, trait_brave, trait_hardworking, trait_lazy
			EmployeeStats.declare(in);    // low_health, social, health, danger, fatigue, hunger, work
		});

		// Mixes must live in a layer: probe() only computes layers, so a mix
		// declared with t.mix() outside one is never evaluated.
		t.layer("drives", layer -> {
			// --- protection (kept from the fight example) ---
			// proximity = 1 - targetDist
			layer.mix("proximity")
				.bias(1.0)
				.sub("targetDist", 1.0);

			// anger = proximity * health: close and healthy -> attack.
			layer.mix("anger")
				.bias(1.0)
				.mul("proximity", 1.0)
				.mul("health", 1.0)
				.onHigh(Drives.ANGER);

			// weak = 1 - health
			layer.mix("weak")
				.bias(1.0)
				.sub("health", 1.0);

			// fear = proximity * weak: close and hurt -> flee.
			layer.mix("fear")
				.bias(1.0)
				.mul("proximity", 1.0)
				.mul("weak", 1.0)
				.onHigh(Drives.FEAR);

			// --- employee life (drives fire off stats, colored by traits) ---

			// hunger to eat: fed by hunger & poor health, damped by danger/fatigue.
			layer.mix("desire_eat")
				.add("hunger", 1.2)
				.add("low_health", 1.5)
				.sub("danger", 0.5)
				.sub("fatigue", 0.2)
				.sigmoid(7)
				.onHigh(Drives.HUNGER);

			// desire to rest: needs sleep, made stronger by laziness.
			layer.mix("desire_rest")
				.add("fatigue", 1.2)
				.add("trait_lazy", 0.4)
				.sub("hunger", 0.3)
				.sub("danger", 0.5)
				.sigmoid(6)
				.onHigh(Drives.FATIGUE);
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
				s -> 1.0))

			// eat: walk to the food station, then bite from a chest there.
			// The plan-fiction flags (atFood/ate) order the steps; the tasks
			// re-check the real world themselves, so sloppy fiction is safe.
			.addAction(new GoapAction("goto_food",
				s -> s.get("ate") < 1f && s.get("atFood") < 1f,
				s -> s.set("atFood", 1f),
				s -> 1.0))
			.addAction(new GoapAction("eat",
				s -> s.get("atFood") >= 1f && s.get("ate") < 1f,
				s -> s.set("ate", 1f),
				s -> 1.0))

			// rest: walk to the rest station, then stay put until the fatigue
			// drains away (the mob feeds stats.rest() while near the bed).
			.addAction(new GoapAction("goto_rest",
				s -> s.get("rested") < 1f && s.get("atRest") < 1f,
				s -> s.set("atRest", 1f),
				s -> 1.0))
			.addAction(new GoapAction("rest",
				s -> s.get("atRest") >= 1f && s.get("rested") < 1f,
				s -> s.set("rested", 1f),
				s -> 2.0));
	}

	private static DriveGoals buildGoals() {
		return (drive, world) -> {
			Optional<Goal> goal;
			if (drive == Drives.HUNGER) goal = Optional.of(Goal.of(s -> s.get("ate") >= 1f, world));
			else if (drive == Drives.FATIGUE) goal = Optional.of(Goal.of(s -> s.get("rested") >= 1f, world));
			else if (drive == Drives.ANGER) goal = Optional.of(Goal.of(s -> s.get("attacked") >= 1f, world));
			else if (drive == Drives.FEAR) goal = Optional.of(Goal.of(s -> s.get("safe") >= 1f, world));
			else if (drive == WANDER) goal = Optional.of(Goal.of(s -> s.get("wandered") >= 1f, world));
			else goal = Optional.empty();

			return goal;
		};
	}
}
