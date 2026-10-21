package deus.paperwork.block.file_cabinet;

import com.mojang.logging.LogUtils;
import deus.paperwork.block.file_cabinet.TileEntityFileCabinet;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogicRotatable;
import net.minecraft.core.block.entity.TileEntityChest;
import net.minecraft.core.block.material.Material;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.player.inventory.container.Container;
import net.minecraft.core.util.helper.Direction;
import net.minecraft.core.util.helper.Side;
import net.minecraft.core.util.helper.DyeColor;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public class BlockFileCabinetLogic extends BlockLogicRotatable {

	public BlockFileCabinetLogic(Block<?> block, Material material) {
		super(block, material);
		block.withEntity(TileEntityFileCabinet::new);
	}


	@Override
	public boolean onBlockRightClicked(World world, int x, int y, int z, Player player, Side side, double xPlaced, double yPlaced) {
		if (world.isClientSide) return true;
		player.displayChestScreen(getInventory(world, x, y, z), (double)x, (double)y, (double)z);
		return true;
	}

	public static Container getInventory(World world, int x, int y, int z) {
		return (Container) world.getTileEntity(x, y, z);
	}

	@Override
	public boolean isSolidRender() {
		return false;
	}


}
