package deus.paperwork.core.ai.pathfinding;

import de.bsommerfeld.pathetic.api.pathing.processing.ValidationProcessor;
import de.bsommerfeld.pathetic.api.pathing.processing.context.EvaluationContext;
import de.bsommerfeld.pathetic.api.wrapper.PathPosition;
import net.minecraft.core.block.BlockLogicFlower;
import net.minecraft.core.block.tag.BlockTags;
import net.minecraft.core.world.World;
import net.minecraft.core.world.pos.TilePos;

public class EmployeeWalkValidator implements ValidationProcessor {

	private final World world;

	public EmployeeWalkValidator(World world) {
		this.world = world;
	}

	@Override
	public boolean isValid(EvaluationContext context) {
		PathPosition pos = context.getCurrentPathPosition();
		TilePos current  = new TilePos().set(pos.getX(), pos.getY(),     pos.getZ());
		TilePos below    = new TilePos().set(pos.getX(), pos.getY() - 1, pos.getZ());
		TilePos below2   = new TilePos().set(pos.getX(), pos.getY() - 2, pos.getZ());

		var currentBlock = world.getBlockType(current);
		var belowBlock   = world.getBlockType(below);

		boolean currentPassable = !currentBlock.isCollidable()
			|| currentBlock.getLogic() instanceof BlockLogicFlower;

		boolean hasFloor = (belowBlock.isCollidable()
			&& !(belowBlock.getLogic() instanceof BlockLogicFlower))
			|| (world.getBlockType(below2).isCollidable());

		boolean lava  = currentBlock.hasTag(BlockTags.IS_LAVA)  || belowBlock.hasTag(BlockTags.IS_LAVA);
		boolean water = currentBlock.hasTag(BlockTags.IS_WATER) || belowBlock.hasTag(BlockTags.IS_WATER);

		return currentPassable && hasFloor && !lava && !water;
	}
}
