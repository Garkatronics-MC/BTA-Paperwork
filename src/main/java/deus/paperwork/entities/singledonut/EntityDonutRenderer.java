package deus.paperwork.entities.singledonut;

import deus.paperwork.entities.base.EntityRendererDefault;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.tessellator.TessellatorGeneral;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.useless.dragonfly.models.entity.StaticEntityModel;

public class EntityDonutRenderer extends EntityRendererDefault<EntityDonut> {


	@Override
	protected void loadTexture(EntityDonut entity) {
		this.bindTexture("/assets/paperwork/textures/entity/donut/" + entity.getTextureReference() + "1.png");
	}

	@Override
	protected StaticEntityModel getModel(EntityDonut entity, float partialTick) {
		return this.getModel("main");
	}
}
