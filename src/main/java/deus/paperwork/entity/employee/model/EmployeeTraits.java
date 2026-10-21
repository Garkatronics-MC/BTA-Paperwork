package deus.paperwork.entity.employee.model;

import deus.brainless.agent.Agent;
import deus.brainless.limbic.InputBuilder;

import java.util.Random;

/**
 * Permanent personality features of an employee. Immutable — they never change
 * once the employee is created, which is why they live in the agent's
 * {@code Temperament} as constant inputs.
 *
 * Two touch points with the temperament:
 * <ul>
 *   <li>{@link #declare(InputBuilder)} — registers the trait inputs on the
 *       {@code Temperament} template (once, at template build time).</li>
 *   <li>{@link #set(Agent)} — writes the instance's constant values onto a
 *       live agent (once, right after the agent is built).</li>
 * </ul>
 *
 * <pre>{@code
 * // template (static):
 * traitsTemplate().declare(ib -> ...);
 * // per entity (constant):
 * traits.set(agent);
 * }</pre>
 */
public record EmployeeTraits(double social, double brave, double hardworking, double lazy) {

	public EmployeeTraits {
		social      = clamp(social);
		brave       = clamp(brave);
		hardworking = clamp(hardworking);
		lazy        = clamp(lazy);
	}

	/// Random traits, matching the vanilla spawn roll: every trait lands in [0.3, 1.0].
	public static EmployeeTraits random() {
		return random(new Random());
	}

	public static EmployeeTraits random(Random rng) {
		return new EmployeeTraits(
			0.3 + rng.nextDouble() * 0.7,
			0.3 + rng.nextDouble() * 0.7,
			0.3 + rng.nextDouble() * 0.7,
			0.3 + rng.nextDouble() * 0.7
		);
	}

	/// All traits at a uniform value, for scripted/professional employees.
	public static EmployeeTraits uniform(double value) {
		return new EmployeeTraits(value, value, value, value);
	}

	/** Registers the trait inputs on a {@code Temperament} template (default values until set). */
	public static void declare(InputBuilder ib) {
		ib.declare("trait_social", 0.5)
			.declare("trait_brave", 0.5)
			.declare("trait_hardworking", 0.5)
			.declare("trait_lazy", 0.5);
	}

	/** Writes this employee's constant trait values onto a live agent, never changing again. */
	public void set(Agent agent) {
		agent.sense("trait_social", social);
		agent.sense("trait_brave", brave);
		agent.sense("trait_hardworking", hardworking);
		agent.sense("trait_lazy", lazy);
	}

	private static double clamp(double value) {
		return Math.min(1.0, Math.max(0.0, value));
	}
}
