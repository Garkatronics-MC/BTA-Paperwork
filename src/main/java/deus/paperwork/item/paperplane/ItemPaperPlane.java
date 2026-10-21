package deus.paperwork.item.paperplane;

import deus.paperwork.entity.paperplane.EntityPaperPlane;
import deus.paperwork.item.base.ItemColored;
import deus.utils.annotations.RegisterItemModel;
import net.minecraft.client.render.item.model.ItemModelStandard;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemPaintBrush;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.util.helper.DyeColor;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static deus.paperwork.Paperwork.MOD_ID;

@RegisterItemModel(model = ItemModelStandard.class)
public class ItemPaperPlane extends ItemColored {
	public ItemPaperPlane(String translationKey, String namespaceId, int id) {
		super(translationKey, namespaceId, id);
		setMaxStackSize(1);
	}

	@Override
	public @Nullable ItemStack onUse(@NotNull ItemStack selfStack, @NotNull World world, @NotNull Player player) {
		EntityPaperPlane paperPlane = new EntityPaperPlane(world);
		world.playSoundAtEntity(null, player, MOD_ID+":material.paper.soft",  1.0F, 1.0f);

		DyeColor newColor = ItemPaintBrush.getColor(selfStack);
		paperPlane.setColor(newColor);
		paperPlane.spawnInit();

		// Throw it
		float yaw = player.yRot * 0.017453292F;
		float pitch = player.xRot * 0.017453292F;
		float speed = 0.95F;

		paperPlane.moveTo(player.x, player.y, player.z, player.yRot, 0);

		paperPlane.xd = -MathHelper.sin(yaw) * MathHelper.cos(pitch) * speed;
		paperPlane.yd = -MathHelper.sin(pitch) * (speed/1.5);
		paperPlane.zd = MathHelper.cos(yaw) * MathHelper.cos(pitch) * speed;

		world.entityJoinedWorld(paperPlane);
		selfStack.consumeItem(player);
		return super.onUse(selfStack, world, player);
	}
}
