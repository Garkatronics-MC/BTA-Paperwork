package deus.paperwork.entities.paperplane;

import deus.paperwork.entities.base.EntityRendererDefault;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.renderer.GLRenderer;
import net.minecraft.client.render.tessellator.Tessellator;
import net.minecraft.client.render.tessellator.TessellatorGeneral;
import net.minecraft.core.util.helper.DyeColor;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.useless.dragonfly.models.entity.StaticEntityModel;

public class PaperPlaneRenderer extends EntityRendererDefault<EntityPaperPlane> {

	private final String DEFAULT_TEXTURE = "/assets/paperwork/textures/entity/paperplane/paperplane.png";


	@Override
	protected void loadTexture(EntityPaperPlane entity) {
		String currentTexture = DEFAULT_TEXTURE;
		DyeColor color = entity.getColor();
		if (color != null) {
			currentTexture = "/assets/paperwork/textures/entity/paperplane/" + entity.getColor().colorID + "1.png";
		}

		this.bindTexture(currentTexture);

		switch (entity.getWetState()) {
			case DRY:
				GLRenderer.setColor4f(1.0F, 1.0F, 1.0F, 1.0F);
				break;
			case WET:
				GLRenderer.setColor4f(0.7F, 0.7F, 0.7F, 1.0F);
				break;
			case VERY_WET:
				GLRenderer.setColor4f(0.4F, 0.4F, 0.4F, 1.0F);
				break;
		}
	}

	@Override
	protected StaticEntityModel getModel(EntityPaperPlane entity, float partialTick) {
		return this.getModel("main");
	}
}


