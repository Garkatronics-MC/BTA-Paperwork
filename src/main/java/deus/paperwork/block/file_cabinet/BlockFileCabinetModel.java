package deus.paperwork.block.file_cabinet;

import net.minecraft.client.render.block.model.BlockModelHorizontalRotation;
import net.minecraft.client.render.block.model.BlockModelStandard;
import net.minecraft.client.render.tessellator.Tessellator;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogic;
import net.minecraft.core.util.helper.Side;
import net.minecraft.core.util.phys.AABB;
import net.minecraft.core.world.WorldSource;
import net.minecraft.core.world.pos.TilePos;
import net.minecraft.core.world.pos.TilePosc;
import org.jetbrains.annotations.NotNull;
import org.joml.primitives.AABBdc;

import static deus.paperwork.Paperwork.MOD_ID;


public class BlockFileCabinetModel <T extends BlockLogic> extends BlockModelHorizontalRotation<T> {
	public BlockFileCabinetModel(Block<T> block) {
		super(block);
	}

	@Override
	public boolean shouldSideBeRendered(@NotNull WorldSource source, @NotNull AABBdc bounds, @NotNull TilePosc tilePos, @NotNull Side side) {
		return true;
	}

	@Override
	public boolean shouldSideBeRendered(@NotNull WorldSource source, @NotNull AABBdc bounds, @NotNull TilePos tilePos, @NotNull Side side, int meta) {
		return true;
	}
}
