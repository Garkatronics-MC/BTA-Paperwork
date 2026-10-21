package deus.paperwork.entity.base;

import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.renderer.BlendFactor;
import net.minecraft.client.render.renderer.GLRenderer;
import net.minecraft.client.render.renderer.State;
import net.minecraft.client.render.tessellator.TessellatorGeneral;
import net.minecraft.core.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.useless.dragonfly.models.entity.StaticEntityModel;

public abstract class EntityRendererDefault<T extends Entity> extends EntityRenderer<T> {

	@Override
	public void render(@NotNull TessellatorGeneral tessellator, @NotNull T entity,
	                   double x, double y, double z, float yaw, float partialTick) {

		GLRenderer.pushFrame();
		GLRenderer.enableState(State.BLEND);
		GLRenderer.setBlendFunc(BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA);
		GLRenderer.disableState(State.CULL_FACE);
		GLRenderer.setLightmapCoord2i(15, 15);

		this.loadTexture(entity);
		this.applyTransform(entity, x, y, z, yaw, partialTick);

		StaticEntityModel model = this.getModel(entity, partialTick);
		if (model != null) {
			model.render();
		}

		GLRenderer.enableState(State.CULL_FACE);
		GLRenderer.disableState(State.BLEND);
		GLRenderer.popFrame();
	}

	/** Bind texture for this entity. */
	protected abstract void loadTexture(T entity);

	/** Return configured model ready to render, or null to skip. */
	protected abstract StaticEntityModel getModel(T entity, float partialTick);

	/** Override to customize GL transform. Default: position + yaw. */
	protected void applyTransform(T entity, double x, double y, double z, float yaw, float partialTick) {
		GLRenderer.modelM4f().translate((float)x, (float)y, (float)z);
		GLRenderer.modelM4f().rotateY(org.joml.Math.toRadians(yaw));
		GLRenderer.modelM4f().scale(0.0625F, 0.0625F, -0.0625F);
	}
}
