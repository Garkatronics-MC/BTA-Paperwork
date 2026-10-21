package deus.paperwork.item.custom_paper;

import deus.utils.annotations.RegisterItemModel;
import net.minecraft.client.render.item.model.ItemModelStandard;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemDye;
import net.minecraft.core.item.ItemPaintBrush;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.player.inventory.slot.Slot;
import net.minecraft.core.util.helper.DyeColor;
import net.minecraft.core.world.World;

@RegisterItemModel(model = ItemCustomPaperModel.class)
public class ItemCustomPaper extends Item {

	private boolean shouldDye = false;
	public ItemCustomPaper(String translationKey, String namespaceId, int id) {
		super(translationKey, namespaceId, id);
	}

	@Override
	public void inventoryTick(ItemStack itemstack, World world, Entity entity, int slotId, boolean flag) {
		ItemStack grabbedItem = ((Player)entity).inventory.getHeldItemStack();
		if (grabbedItem != null && grabbedItem.getItem() instanceof ItemDye) {
			shouldDye = true;

		} else {
			shouldDye = false;
		}
		super.inventoryTick(itemstack, world, entity, slotId, flag);
	}

	@Override
	public ItemStack onInventoryInteract(Player player, Slot slot, ItemStack stackInSlot, boolean isItemGrabbed) {
		if (!isItemGrabbed) {
			ItemStack grabbedItem = player.inventory.getHeldItemStack();
			if (grabbedItem != null && grabbedItem.getItem() instanceof ItemDye) {
				shouldDye = true;
				DyeColor currentColor = ItemPaintBrush.getColor(stackInSlot);
				DyeColor newColor = DyeColor.colorFromItemMeta(grabbedItem.getMetadata());
				if (currentColor != newColor) {
					ItemPaintBrush.setColor(stackInSlot, newColor);
					--grabbedItem.stackSize;
					if (grabbedItem.stackSize <= 0) {
						player.inventory.setHeldItemStack(null);
					}
					return stackInSlot;
				}
			}
		}
		return super.onInventoryInteract(player, slot, stackInSlot, isItemGrabbed);
	}
	@Override
	public boolean hasInventoryInteraction() {
		return shouldDye;
	}

}
