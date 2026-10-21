package deus.paperwork.item.letter;

import com.mojang.nbt.tags.CompoundTag;
import deus.paperwork.gui.LetterUI;
import deus.paperwork.registry.PaperworkItems;
import deus.utils.annotations.RegisterItemModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.item.model.ItemModelStandard;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RegisterItemModel(model = ItemModelStandard.class)
public class ItemLetter extends Item {
	public ItemLetter(String translationKey, String namespaceId, int id) {
		super(translationKey, namespaceId, id);
	}

	@Override
	public @Nullable ItemStack onUse(@NotNull ItemStack selfStack, @NotNull World world, @NotNull Player player) {
		if (player.isSneaking()) {
			CompoundTag tag = selfStack.getData();
			if (tag == null) {
				tag = new CompoundTag();
			}

			String text = tag.getString("text");

			CompoundTag newTag = new CompoundTag();
			newTag.putString("text", text);
			newTag.putBoolean("editable", false);

			CompoundTag oldTag = selfStack.getData();
			if (oldTag.containsKey("tox")) {
				newTag.putInt("tox", oldTag.getInteger("tox"));
			}
			if (oldTag.containsKey("toy")) {
				newTag.putInt("toy", oldTag.getInteger("toy"));
			}
			if (oldTag.containsKey("toz")) {
				newTag.putInt("toz", oldTag.getInteger("toz"));
			}

			ItemStack closedLetter = new ItemStack(PaperworkItems.CLOSED_LETTER, 1);
			closedLetter.setData(newTag);

			selfStack.consumeItem(player);
			return closedLetter;
		}

		Minecraft.getMinecraft().displayScreen(new LetterUI(selfStack));
		return selfStack;
	}
}
