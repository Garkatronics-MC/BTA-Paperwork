package deus.paperwork.block.printer;

import com.mojang.nbt.tags.CompoundTag;
import deus.utils.annotations.RegisterEntity;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.entity.TileEntity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.player.inventory.container.Container;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import static deus.paperwork.Paperwork.MOD_ID;

@RegisterEntity(id = "entity_printer", name = "entity_printer")
public class TileEntityPrinter extends TileEntity implements Container {

	protected ItemStack[] itemStacks = new ItemStack[5];
	public int maxPrintTime = 200;
	public int currentPrintTime = maxPrintTime;
	boolean activated = true;

	public TileEntityPrinter() {
		this.itemStacks = new ItemStack[12];
	}

	public void tick() {
		if (this.currentPrintTime > 0) {
			--this.currentPrintTime;
		} else if (activated) {
			print();
		}
	}

	@Override
	public void readAdditionalData(@NotNull CompoundTag compoundTag) {

	}

	@Override
	public void writeAdditionalData(@NotNull CompoundTag compoundTag) {

	}

	public void removeLayer(World world, Block<?> block, int blockX, int blockY, int blockZ) {
		Block<?> targetBlock = world.getBlockType(tilePos);
		int meta = world.getBlockMetadata(blockX, blockY, blockZ);

		if (targetBlock == block && meta > 0) {
			world.setBlockAndMetadataWithNotify(blockX, blockY, blockZ, block.id(), meta - 1);
			//world.playBlockSoundEffect(player, (double)((float)blockX + 0.5F), (double)((float)blockY + 0.5F), (double)((float)blockZ + 0.5F), block, EnumBlockSoundEffectType.BREAK);
		} else if (targetBlock == block && meta == 0) {
			world.setBlockAndMetadataWithNotify(blockX, blockY, blockZ, 0, 0);
			//world.playBlockSoundEffect(player, (double)((float)blockX + 0.5F), (double)((float)blockY + 0.5F), (double)((float)blockZ + 0.5F), block, EnumBlockSoundEffectType.BREAK);
		}

	}

	protected void print() {
		assert worldObj != null;
//		if (worldObj.getBlockType(tilePos).id() == PaperworkBlocks.BLOCK_PAPER_PILE.id()) {
//			removeLayer(worldObj, PaperworkBlocks.BLOCK_PAPER_PILE, tilePos.x(), tilePos.y(),tilePos.z());
//		}
		activated = false;
	}


	@Override public int getContainerSize() {
		return this.itemStacks.length;
	}

	@Override public @Nullable ItemStack getItem(int index) {
		return this.itemStacks[index];
	}

	@Override public @Nullable ItemStack removeItem(int index, int takeAmount) {
		if (this.itemStacks[index] != null) {
			ItemStack itemstack1;
			if (this.itemStacks[index].stackSize <= takeAmount) {
				itemstack1 = this.itemStacks[index];
				this.itemStacks[index] = null;
				if (this.worldObj != null && index == 2) {
					this.worldObj.markBlockNeedsUpdate(tilePos);
				}

				return itemstack1;
			} else {
				itemstack1 = this.itemStacks[index].splitStack(takeAmount);
				if (this.itemStacks[index].stackSize <= 0) {
					this.itemStacks[index] = null;
					if (this.worldObj != null && index == 2) {
						this.worldObj.markBlockNeedsUpdate(tilePos);
					}
				}

				return itemstack1;
			}
		} else {
			return null;
		}
	}

	@Override public void setItem(int index, @Nullable ItemStack itemstack) {
		this.itemStacks[index] = itemstack;
		if (itemstack != null && itemstack.stackSize > this.getMaxStackSize()) {
			itemstack.stackSize = this.getMaxStackSize();
		}

		if (this.worldObj != null && index == 2 && itemstack == null) {
			this.worldObj.markBlockNeedsUpdate(tilePos);
		}

	}

	@Override public @NonNull String getNameTranslationKey() {
		return "container.furnace.name";
	}

	@Override
	public int getMaxStackSize() {
		return 64;
	}

	public boolean stillValid(@NonNull Player player) {
		if (this.worldObj != null && this.worldObj.getTileEntity(tilePos) == this) {
			return player.distanceToSqr((double)this.tilePos.x() + 0.5, (double)this.tilePos.y() + 0.5, (double)this.tilePos.z() + 0.5) <= 64.0;
		} else {
			return false;
		}
	}

	@Override
	public void sort() {

	}


}
