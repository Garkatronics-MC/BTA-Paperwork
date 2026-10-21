package deus.paperwork.block.mailbox;

import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogicTransparent;
import net.minecraft.core.block.material.Material;
import net.minecraft.core.block.material.Materials;

public class BlockMailboxLogic extends BlockLogicTransparent {

	public BlockMailboxLogic(Block<?> block) {
		super(block, Materials.METAL);

	}

	@Override
	public boolean isCubeShaped() {
		return false;
	}
}

