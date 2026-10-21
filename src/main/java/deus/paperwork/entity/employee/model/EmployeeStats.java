package deus.paperwork.entity.employee.model;

import deus.brainless.agent.Agent;
import deus.brainless.limbic.InputBuilder;

/**
 * Volatile well-being measurements of an employee. Mutable — they drift every
 * tick and are fed to the agent by the owner each update, unlike
 * {@link EmployeeTraits} which are constant.
 *
 * The values are kept in the classic [0, 1] range:
 * <ul>
 *   <li>{@code hunger}  — rises over time; lowered by eating (0 = fed, 1 = starving)</li>
 *   <li>{@code fatigue} — rises over time; lowered by resting (0 = rested, 1 = exhausted)</li>
 *   <li>{@code work}    — fuel for working; drains over time away from work, recharges at the workplace (0 = drained, 1 = full)</li>
 *   <li>{@code social}  — falls over time; raised by conversation (0 = sociable, 1 = lonely)</li>
 *   <li>{@code danger}  — surroundings threat level, driven externally (0 = safe, 1 = deadly)</li>
 *   <li>{@code health}  — mirrored from the mob's own health via {@link #syncHealth}</li>
 * </ul>
 */
public final class EmployeeStats {

	private static final double HUNGER_RATE  = 0.0002;   // hunger gained per tick
	private static final double FATIGUE_RATE = 0.0001;   // fatigue gained per tick
	private static final double WORK_RATE    = 0.00015;  // work lost per tick while idle
	private static final double SOCIAL_RATE  = 0.0001;   // social lost per tick

	public double hunger  = 0.8;
	public double fatigue = 0.1;
	public double work    = 0.8;
	public double social  = 0.8;
	public double danger  = 0.0;
	public double health  = 1.0;

	/**
	 * Natural daily drift: gets hungry, gets tired, gets eager to work,
	 * gets lonely. Call once per tick, server side only.
	 */
	public void tick() {
		hunger  = clamp(hunger + HUNGER_RATE);
		fatigue = clamp(fatigue + FATIGUE_RATE);
		work    = clamp(work - WORK_RATE);
		social  = clamp(social - SOCIAL_RATE);
	}

	/** Mirrors the mob's vanilla health into the [0, 1] input. */
	public void syncHealth(float currentHealth, float maxHealth) {
		health = maxHealth <= 0 ? 1.0 : clamp((double) currentHealth / maxHealth);
	}

	/// Everything is gone but danger — the mob is dying and knows it.
	public double lowHealth() {
		return clamp(1.0 - health);
	}

	/// Ate a matching item; recharges by the given amount.
	public void eat(double relief) {
		hunger = clamp(hunger - relief);
	}

	/// Slept near a bed; recharges by the given amount.
	public void rest(double relief) {
		fatigue = clamp(fatigue - relief);
	}

	/// Talked to someone; recharges by the given amount.
	public void chat(double relief) {
		social = clamp(social - relief);
	}

	/// Worked a shift at the workplace; recharges by the given amount.
	public void restoreWork(double relief) {
		work = clamp(work + relief);
	}

	/// Surroundings changed (source fired a projectile, mob is on fire, ...).
	public void setDanger(double danger) {
		this.danger = clamp(danger);
	}

	/** Writes this employee's living stats onto the agent, once per tick. */
	public void set(Agent agent) {
		agent.sense("low_health", lowHealth());
		agent.sense("social", social);
		agent.sense("health", health);
		agent.sense("danger", danger);
		agent.sense("fatigue", fatigue);
		agent.sense("hunger", hunger);
		agent.sense("work", work);
	}

	/** Registers the stat inputs on a Temperament template (defaults until the mob senses them). */
	public static void declare(InputBuilder ib) {
		ib.declare("low_health", 0.0)
			.declare("social", 0.0)
			.declare("health", 1.0)
			.declare("danger", 0.0)
			.declare("fatigue", 0.0)
			.declare("hunger", 0.0)
			.declare("work", 0.0);
	}

	private static double clamp(double value) {
		return Math.min(1.0, Math.max(0.0, value));
	}
}
