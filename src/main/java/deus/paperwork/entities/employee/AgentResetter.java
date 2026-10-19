package deus.paperwork.entities.employee;

import deus.brainless.agent.AgentMob;
import deus.paperwork.mixin.IAgentMob;

/// Rebuilds the brain of an employee when its job changes.
///
/// The Agent of Brainless bakes the temperament, the planner and the goal
/// mapping at construction time, so a new job needs a new agent; AgentMob
/// caches it in a private field, hence the accessor.
public final class AgentResetter {

	private AgentResetter() {}

	/// Stops the running task and drops the cached agent, so the next
	/// agent() call rebuilds it from the current job strategy.
	///
	/// The order matters: clear() first, so the running task (Farm, Eat, ...)
	/// is cancelled while the old brain is still in place; only then the
	/// agent is dropped.
	public static void reset(AgentMob mob) {
		if (mob == null) {
			throw new IllegalArgumentException("Cannot reset the brain of a null mob");
		}
		/// The check looks like dead code (the mixin is always applied), but it
		/// stays: if the mixin ever did not get applied (a config problem, a
		/// conflict with another mod), it turns a cryptic ClassCastException
		/// into an error that says what to do.
		if (!(mob instanceof IAgentMob accessor)) {
			throw new IllegalStateException("Cannot reset the brain of " + mob.getClass().getName()
				+ ": the Brainless agent accessor (deus.paperwork.mixin.IAgentMob) is not applied."
				+ " Is the paperwork mixin config enabled?");
		}
		mob.controller().clear();
		accessor.paperwork$setAgent(null);
	}

}
