package deus.paperwork.entities.cardboard_box;

import deus.paperwork.entities.base.EntityRendererDefault;
import net.minecraft.client.render.renderer.GLRenderer;
import net.minecraft.core.util.helper.DyeColor;
import org.jetbrains.annotations.NotNull;
import org.useless.dragonfly.models.entity.StaticEntityModel;

public class RendererCardboardBox extends EntityRendererDefault<EntityCardboardBox> {

	public RendererCardboardBox() {
	}

	@Override
	protected void loadTexture(EntityCardboardBox entity) {
		DyeColor color = entity.getColor();
		if (color == null) {
			bindTexture(entity.halloween
				? "/assets/paperwork/textures/entity/cardboard_box/halloween/pumpkin.png"
				: "/assets/paperwork/textures/entity/cardboard_box/texture.png");
		} else {
			bindTexture("/assets/paperwork/textures/entity/cardboard_box/gift_box/" + color.colorID + "1.png");
		}
	}

	@Override
	protected StaticEntityModel getModel(EntityCardboardBox entity, float partialTick) {
		return this.getModel("main");
	}

	@Override
	protected void applyTransform(EntityCardboardBox entity, double x, double y, double z, float yaw, float partialTick) {
		float scale = entity.getBoxSize().getScale() / 0.0625F;
		GLRenderer.modelM4f().translate((float) x, (float) y, (float) z);
		GLRenderer.modelM4f().rotateY(org.joml.Math.toRadians(180.0F - yaw));
		GLRenderer.modelM4f().scale(0.0625F * scale, 0.0625F * scale, 0.0625F * scale);
	}
}
