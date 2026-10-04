package deus.paperwork.entities.employee.farmer;

import deus.brainless.ai.AI;
import deus.paperwork.ai.AIHolder;
import deus.paperwork.ai.Brains;
import deus.paperwork.entities.employee.MobEmployee;
import deus.paperwork.util.PoscArea;
import deus.utils.annotations.RegisterEntity;
import deus.utils.annotations.RegisterEntityRenderer;
import net.minecraft.core.block.BlockLogicCropsWheat;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.enums.EnumDropCause;
import net.minecraft.core.item.IBonemealable;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import net.minecraft.core.world.World;
import net.minecraft.core.world.pos.TilePos;
import net.minecraft.core.world.pos.TilePosc;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

@RegisterEntityRenderer(renderer = MobEmployeeFarmerRenderer.class)
@RegisterEntity(id = "employee_farmer", name = "employee_farmer")
public class MobEmployeeFarmer extends MobEmployee  {

	public Optional<PoscArea.Area2D> farm_area = Optional.empty();

	public MobEmployeeFarmer(@NotNull World world) {
		super(world, new AIHolder<>(Brains.EmployeeFarmerAI.get()));
		//this.setTextureIdentifier("paperwork", "employee_farmer");
	}

	@Override
	@SuppressWarnings("unchecked")
	public AI<MobEmployeeFarmer> ai() {
		return (AI<MobEmployeeFarmer>) aiHolder.ai();
	}

	public List<TilePosc> getSowableBlocksInArea(PoscArea.Area2D blockArea) {
		return getBlocksInArea(blockArea, Blocks.FARMLAND_DIRT.id()).stream()
			.filter(p -> {
				TilePosc above = new TilePos(p.x(), p.y() + 1, p.z());
				return world.getBlockType(above).id() == Blocks.AIR.id();
			})
			.toList();
	}
	public List<TilePosc> getHarvestableBlocksInArea(PoscArea.Area2D blockArea) {
		return getBlocksInArea(blockArea).stream().filter(posc -> isHarvestable(new TilePos(posc.x(), posc.y()+1, posc.z()))).toList();
	}

	public boolean isHarvestable(TilePosc pos) {
		if (!(world.getBlockType(pos).getLogic() instanceof IBonemealable)) return false;
		int growState = world.getBlockData(pos);
		return growState == BlockLogicCropsWheat.MAX_GROWTH_STATE;
	}

	public boolean harvest(TilePosc posc) {
		if (!isHarvestable(posc)) return false;
		world.setBlockTypeNotify(posc, Blocks.CROPS_WHEAT);
		ItemStack[] stacks = Blocks.CROPS_WHEAT.getBreakResult(world, EnumDropCause.PICK_BLOCK, 0, null);

		if (stacks == null) return false;
		for (ItemStack stack : stacks) {
			world.dropItem(posc, stack);
		}


		return true;
	}

	public boolean seed(TilePosc posc) {
		ItemStack stack = this.inventory.findStackOf(Items.SEEDS_WHEAT);
		if (stack == null) return false;
		stack.consumeItem(null);

		int blockId = world.getBlockType(posc).id();
		if (blockId != Blocks.AIR.id()) return false;

		world.setBlockTypeNotify(posc, Blocks.CROPS_WHEAT);
		return true;
	}

}
