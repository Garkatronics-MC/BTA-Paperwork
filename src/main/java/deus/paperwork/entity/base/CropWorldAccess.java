package deus.paperwork.entity.base;

import deus.paperwork.core.util.PoscArea;
import net.minecraft.core.block.BlockLogicCropsWheat;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.enums.EnumDropCause;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.item.IBonemealable;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import net.minecraft.core.world.pos.TilePos;
import net.minecraft.core.world.pos.TilePosc;

import java.util.List;

/// The agricultural way of touching the world: reading a farm area
/// (sowable / harvestable tiles) and working it (harvest crops, sow seeds
/// from the mob's own inventory). Named after the domain, not the role:
/// any mob that holds an inventory can crop, a farmer is just its usual user.
///
/// A stateless utility, not an entity component: every call passes the mob
/// (and inventory where needed), so it works for any mob without a per-entity
/// instance. The caller that wires mob/inventory is the task driving the work
/// (e.g. {@link deus.paperwork.core.ai.tasks.Farm}).
public final class CropWorldAccess {

	private CropWorldAccess() {}

	public static List<TilePosc> getSowableBlocksInArea(Mob mob, PoscArea.Area2D blockArea) {
		return MobWorldAccess.getBlocksInArea(mob.world, blockArea, Blocks.FARMLAND_DIRT.id()).stream()
			.filter(p -> {
				TilePosc above = new TilePos(p.x(), p.y() + 1, p.z());
				return mob.world.getBlockType(above).id() == Blocks.AIR.id();
			})
			.toList();
	}

	public static List<TilePosc> getHarvestableBlocksInArea(Mob mob, PoscArea.Area2D blockArea) {
		return PoscArea.tiles(blockArea).stream()
			.filter(posc -> isHarvestable(mob, new TilePos(posc.x(), posc.y() + 1, posc.z())))
			.toList();
	}

	public static boolean isHarvestable(Mob mob, TilePosc pos) {
		if (!(mob.world.getBlockType(pos).getLogic() instanceof IBonemealable)) return false;
		int growState = mob.world.getBlockData(pos);
		return growState == BlockLogicCropsWheat.MAX_GROWTH_STATE;
	}

	public static boolean harvest(Mob mob, TilePosc posc) {
		if (!isHarvestable(mob, posc)) return false;

		// read the grown crop's data before replanting — getBreakResult keys on
		// the growth state: MAX_GROWTH_STATE drops wheat, anything less drops
		// seeds only.
		int blockData = mob.world.getBlockData(posc);

		// replant immediately, then drop the harvest as a natural break.
		mob.world.setBlockTypeNotify(posc, Blocks.CROPS_WHEAT);
		ItemStack[] stacks = Blocks.CROPS_WHEAT.getBreakResult(
			mob.world,
			EnumDropCause.WORLD,
			blockData,
			null
		);

		if (stacks == null) return false;
		for (ItemStack stack : stacks) {
			mob.world.dropItem(posc, stack);
		}

		return true;
	}

	public static boolean seed(Mob mob, ContainerMob inventory, TilePosc posc) {
		// tile must be empty before anything is spent — otherwise a blocked
		// tile would silently eat the seed.
		if (mob.world.getBlockType(posc).id() != Blocks.AIR.id()) return false;

		ItemStack stack = inventory.findStackOf(Items.SEEDS_WHEAT);
		if (stack == null) return false;
		stack.consumeItem(null);

		mob.world.setBlockTypeNotify(posc, Blocks.CROPS_WHEAT);
		return true;
	}
}
