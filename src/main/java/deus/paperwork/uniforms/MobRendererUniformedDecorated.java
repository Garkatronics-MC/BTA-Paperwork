package deus.paperwork.uniforms;

import deus.paperwork.interfaces.ICustomizable;
import net.minecraft.client.render.entity.MobRendererBiped;
import net.minecraft.client.render.renderer.GLRenderer;
import net.minecraft.client.render.tessellator.TessellatorGeneral;
import net.minecraft.core.entity.Mob;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;


public abstract class MobRendererUniformedDecorated<T extends Mob & ICustomizable>
	extends MobRendererBiped<T> {

	private static final UniformShape[] UNIFORM_LAYERS = {
		UniformShape.SHIRT,
		UniformShape.PANTS,
		UniformShape.HAT,
		UniformShape.GLASSES
	};

	protected static final int HAIR_LAYER = UNIFORM_LAYERS.length + 1;

	public MobRendererUniformedDecorated(float shadowSize) {
		super(shadowSize);
	}

	@Override
	protected @Nullable StaticEntityModel getAndSetupModelForLayer(@NotNull T entity, float brightness, float partialTick, int layer) {
		if (layer == 0) {
			return super.getAndSetupModelForLayer(entity, brightness, partialTick, layer);
		}

		if (layer <= UNIFORM_LAYERS.length) {
			return setupUniformLayer(entity, partialTick, layer);
		}

		if (layer == HAIR_LAYER) {
			return setupHairLayer(entity, partialTick);
		}

		return null;
	}

	private @Nullable StaticEntityModel setupUniformLayer(@NotNull T entity, float partialTick, int layer) {
		UniformShape slot = UNIFORM_LAYERS[layer - 1];
		UniformPiece piece = entity.getUniformPiece(slot);
		if (piece == null) return null;

		this.bindTexture(resolveUniformTexture(piece));
		StaticEntityModel model = this.getModel("uniform");
		if (model == null) return null;

		StaticEntityModel result = this.setupAnimations(entity, model, partialTick, layer);
		applyUniformVisibility(model, slot);
		return result;
	}

	private @Nullable StaticEntityModel setupHairLayer(@NotNull T entity, float partialTick) {
		HairPiece hairPiece = entity.getHairPiece();
		if (hairPiece.style() == HairStyle.NONE) return null;
		if (hairPiece.style().modelId == null) return null;

		this.bindTexture(hairPiece.resolveTexture());
		StaticEntityModel model = this.getModel(hairPiece.style().modelId);
		if (model == null) return null;

		StaticEntityModel baseModel = this.getActiveModel(entity);
		if (baseModel != null) {
			BoneTransform baseHead = baseModel.getTransform("head");
			BoneTransform hairRoot = model.getTransform("root");
			hairRoot.rotX = baseHead.rotX;
			hairRoot.rotY = baseHead.rotY;
			hairRoot.rotZ = baseHead.rotZ;
			hairRoot.posX = baseHead.posX;
			hairRoot.posY = baseHead.posY;
			hairRoot.posZ = baseHead.posZ;
		}

		if (hairPiece.hasTint()) {
			int rgb = hairPiece.rgb();
			float r = ((rgb >> 16) & 0xFF) / 255.0f;
			float g = ((rgb >> 8)  & 0xFF) / 255.0f;
			float b = ( rgb        & 0xFF) / 255.0f;
			GLRenderer.setColor3f(r, g, b);
		}

		return model;
	}

	@Override
	protected @Nullable StaticEntityModel setupAnimations(@NonNull T entity, @Nullable StaticEntityModel model, float partialTick, int layer) {
		return super.setupAnimations(entity, model, partialTick, layer);
	}


	private void applyUniformVisibility(@NotNull StaticEntityModel model, @NotNull UniformShape slot) {
		model.getTransform("head").visible      = false;
		model.getTransform("torso").visible     = false;
		model.getTransform("rightArm").visible  = false;
		model.getTransform("leftArm").visible   = false;
		model.getTransform("rightLeg").visible  = false;
		model.getTransform("leftLeg").visible   = false;

		switch (slot) {
			case SHIRT -> {
				model.getTransform("torso").visible    = true;
				model.getTransform("rightArm").visible = true;
				model.getTransform("leftArm").visible  = true;
			}
			case PANTS -> {
				model.getTransform("rightLeg").visible = true;
				model.getTransform("leftLeg").visible  = true;
			}
			case HAT, GLASSES -> {
				model.getTransform("head").visible = true;
			}
		}
	}


	/**
	 * Resuelve la textura a usar para una pieza de uniforme.
	 * Usa la textura dinámica si está disponible, si no la por defecto.
	 */
	protected String resolveUniformTexture(@NotNull UniformPiece piece) {
		// Por ahora devuelve la referencia directa.
		// Cuando se implemente la textura dinámica, aquí se consultará la cache.
		return piece.textureRef();
	}

	@Override
	protected int maxRenderLayer(@NotNull T entity) {
		return HAIR_LAYER;
	}
}
