package deus.paperwork.item.letter;

import com.mojang.nbt.tags.CompoundTag;
import deus.paperwork.registry.PaperworkItems;
import deus.utils.annotations.RegisterItemModel;
import net.minecraft.client.render.item.model.ItemModelStandard;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RegisterItemModel(model = ItemModelStandard.class)
public class ItemClosedLetter extends Item {
	public ItemClosedLetter(String translationKey, String namespaceId, int id) {
		super(translationKey, namespaceId, id);
	}

	@Override
	public @Nullable ItemStack onUse(@NotNull ItemStack selfStack, @NotNull World world, @NotNull Player player) {
		if (player.isSneaking()) {

			CompoundTag oldTag = selfStack.getData();
			CompoundTag newTag = new CompoundTag();
			newTag.putString("text", oldTag.getString("text"));
			newTag.putBoolean("editable", false);

			ItemStack letter = new ItemStack(PaperworkItems.LETTER, 1);
			letter.setData(newTag);

			if (oldTag.containsKey("tox")) {
				newTag.putInt("tox", oldTag.getInteger("tox"));
			}
			if (oldTag.containsKey("toy")) {
				newTag.putInt("toy", oldTag.getInteger("toy"));
			}
			if (oldTag.containsKey("toz")) {
				newTag.putInt("toz", oldTag.getInteger("toz"));
			}


			selfStack.consumeItem(player);
			return letter;
		}
		return selfStack;
	}
}
