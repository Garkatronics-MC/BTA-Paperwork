package deus.paperwork.block.confetti;

import deus.paperwork.registry.PaperworkMaterials;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogic;
import net.minecraft.core.block.entity.TileEntity;
import net.minecraft.core.enums.EnumDropCause;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.world.World;

public class ConfettiLogic extends BlockLogic {
	public ConfettiLogic(Block<?> block) {
		super(block, PaperworkMaterials.PAPER);
		setBlockBounds(0,0.0,0f,1,0,1);

	}
	public boolean isSolidRender() {
		return false;
	}

	@Override
	public boolean isCubeShaped() {return false;}


	public ItemStack[] getBreakResult(World world, EnumDropCause dropCause, int meta, TileEntity tileEntity) {
		switch (dropCause) {
			case SILK_TOUCH:
			case EXPLOSION:
			case PROPER_TOOL:
				return new ItemStack[]{new ItemStack(this, meta + 1)};
			case PICK_BLOCK:
				return new ItemStack[]{new ItemStack(this)};
		}
		return new ItemStack[0];
	}


}
