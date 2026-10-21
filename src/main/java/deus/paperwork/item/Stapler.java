package deus.paperwork.item;

import deus.paperwork.entity.clippy.MobClippy;
import deus.paperwork.entity.stapler_projectile.StaplerProjectile;
import deus.paperwork.registry.PaperworkItems;
import deus.utils.annotations.RegisterItemModel;
import net.minecraft.client.render.item.model.ItemModelStandard;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.world.World;
import org.jspecify.annotations.NonNull;

@RegisterItemModel(model = ItemModelStandard.class)
public class Stapler extends Item {
	public Stapler(String translationKey, String namespaceId, int id) {
		super(translationKey, namespaceId, id);
	}

	@Override
	public ItemStack onUse(@NonNull ItemStack itemstack, @NonNull World world, Player entityplayer) {
		if (entityplayer.inventory.consumeInventoryItem(PaperworkItems.IRON_NUGGET.id)) {
			itemstack.damageItem(1, entityplayer);
			world.playSoundAtEntity(entityplayer, entityplayer, "random.bow", 0.3F, 1.0F / (itemRand.nextFloat() * 0.4F + 0.8F));
			if (!world.isClientSide) {
				world.entityJoinedWorld(new StaplerProjectile(world, entityplayer));

				if (itemRand.nextInt(100000) < 1) {
					MobClippy clippy = new MobClippy(world);
					clippy.setPos(entityplayer.x, entityplayer.y, entityplayer.z);
					world.entityJoinedWorld(clippy);

				}

			}
		}

		return itemstack;
	}
}
