package deus.paperwork.item;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.item.model.ItemModelStandard;
import net.minecraft.client.render.texture.stitcher.IconCoordinate;
import net.minecraft.client.render.texture.stitcher.TextureRegistry;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class JobStickModel extends ItemModelStandard {

	/// The vanilla stick texture: this stick is just a stick that cycles jobs.
	public static final IconCoordinate STICK_ICON = TextureRegistry.getTexture("minecraft:item/stick");

	/// The constructor the item model initializer looks up.
	public JobStickModel(@NotNull Item item) {
		super(item, null);
	}

	public JobStickModel(@NotNull Item item, @Nullable String namespace) {
		super(item, namespace);
	}

	@Override
	public @NotNull IconCoordinate getIcon(@Nullable Entity entity, ItemStack itemStack) {
		return STICK_ICON;
	}
}
