package deus.paperwork.item.big_paperplane;

import deus.paperwork.entity.big_paperplane.EntityBigPaperPlane;
import deus.paperwork.item.base.ItemColored;
import deus.utils.annotations.RegisterItemModel;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemPaintBrush;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.util.helper.DyeColor;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static deus.paperwork.Paperwork.MOD_ID;

@RegisterItemModel(model = ItemBigPaperPlaneModel.class)
public class ItemBigPaperPlane extends ItemColored {
	public ItemBigPaperPlane(String translationKey, String namespaceId, int id) {
		super(translationKey, namespaceId, id);
		setMaxStackSize(1);
	}

	@Override
	public @Nullable ItemStack onUse(@NotNull ItemStack selfStack, @NotNull World world, @NotNull Player player) {
		EntityBigPaperPlane paperPlane = new EntityBigPaperPlane(world);
		world.playSoundAtEntity(null, player, MOD_ID+":material.paper.soft",  1.0F, 1.0f);

		DyeColor newColor = ItemPaintBrush.getColor(selfStack);

		paperPlane.setColor(newColor);

		paperPlane.spawnInit();

		// Throw it
		float yaw = player.yRot * 0.017453292F;
		float pitch = player.xRot * 0.017453292F;
		float speed = 1.25F;

		paperPlane.moveTo(player.x, player.y, player.z, player.yRot, 0);


		paperPlane.xd = -MathHelper.sin(yaw) * MathHelper.cos(pitch) * speed;
		paperPlane.yd = -MathHelper.sin(pitch) * (speed/1.5);
		paperPlane.zd = MathHelper.cos(yaw) * MathHelper.cos(pitch) * speed;

		world.entityJoinedWorld(paperPlane);
		selfStack.consumeItem(player);

		if (!world.isClientSide) {
			player.startRiding(paperPlane);
		}

		return super.onUseItem(selfStack, world, player);
	}
}
