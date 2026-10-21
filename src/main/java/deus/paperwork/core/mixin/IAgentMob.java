package deus.paperwork.core.mixin;

import deus.brainless.agent.Agent;
import deus.brainless.agent.AgentMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/// Accessor for the brain of a Brainless mob.
///
/// AgentMob bakes the temperament, the planner and the goal mapping into its
/// Agent, so a new job needs a new agent. The cached agent lives in a private
/// field without a setter, so this accessor is the only way to drop it; the
/// next agent() call rebuilds it from the current job strategy.
///
/// Brainless is a mod, not Minecraft: remapping is off, or the field name
/// would be searched in the Minecraft mappings.
@Mixin(value = AgentMob.class, remap = false)
public interface IAgentMob {

	@Mutable
	@Accessor("agent")
	void paperwork$setAgent(Agent agent);

}
