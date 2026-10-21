package deus.paperwork.item.custom_paper;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.item.model.ItemModelStandard;
import net.minecraft.client.render.texture.stitcher.IconCoordinate;
import net.minecraft.client.render.texture.stitcher.TextureRegistry;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemPaintBrush;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.util.helper.DyeColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

@Environment(EnvType.CLIENT)
public class ItemCustomPaperModel extends ItemModelStandard {
	public static IconCoordinate emptyIcon = TextureRegistry.getTexture("paperwork:item/paper/paper");
	public static IconCoordinate[] paintbrushIcons = new IconCoordinate[16];


	public @NotNull IconCoordinate getIcon(@Nullable Entity entity, ItemStack itemStack) {
		DyeColor color = ItemPaintBrush.getColor(itemStack);
		return color == null ? emptyIcon : paintbrushIcons[color.itemMeta];
	}

	static {
		DyeColor c;
		for(Iterator var0 = DyeColor.itemOrderedColors().iterator(); var0.hasNext(); paintbrushIcons[c.itemMeta] = TextureRegistry.getTexture("paperwork:item/paper/" + c.colorID)) {
			c = (DyeColor)var0.next();
		}

	}
	public ItemCustomPaperModel(Item item, String namespace) {
		super(item, namespace);
	}

}
