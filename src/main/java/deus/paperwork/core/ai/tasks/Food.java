package deus.paperwork.core.ai.tasks;

import net.minecraft.core.world.pos.TilePosc;

import java.util.Optional;

/// Seam for "where does this employee get food" — the world-specific job of
/// finding a stocked chest and actually eating from it. The old MobEmployee
/// used to know both; whoever composes the entity supplies an implementation
/// (e.g. wrapping getBlocksInArea + getChest + eatFoodInChest).
public interface Food {

	/// A chest tile worth walking to and eating from; empty if nothing is stocked.
	Optional<TilePosc> nearestChest(double mobX, double mobZ);

	/// One bite attempt from that chest; true if something was eaten.
	boolean eatFrom(TilePosc chest);
}
