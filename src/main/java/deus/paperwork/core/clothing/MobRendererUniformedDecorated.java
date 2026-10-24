package deus.paperwork.core.clothing;

import deus.paperwork.Paperwork;
import deus.paperwork.core.api.ICustomizable;
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

	/// (debug temporal) combos de empleado ya logueados, para no spamear el log.
	private static final java.util.Set<String> DBG_SEEN = java.util.Collections.synchronizedSet(new java.util.HashSet<>());

	/// (debug temporal) marca de un combo para el log una sola vez.
	protected static void dbgOnce(String key, String msg) {
		if (DBG_SEEN.add(key)) {
			Paperwork.LOGGER.info("[dbg] {}", msg);
		}
	}

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
			StaticEntityModel body = super.getAndSetupModelForLayer(entity, brightness, partialTick, layer);
			hideCoveredBones(entity, body);
			return body;
		}

		if (layer <= UNIFORM_LAYERS.length) {
			return setupUniformLayer(entity, partialTick, layer);
		}

		if (layer == HAIR_LAYER) {
			return setupHairLayer(entity, partialTick);
		}

		return null;
	}

	/// The bones of the body that the uniform of the employee covers.
	///
	/// Every piece of the uniform is drawn exactly where the body part it
	/// replaces is (the jacket on the torso, the sleeves on the arms, the
	/// pants on the legs, the hat on the head), so the skin must not be drawn
	/// under them: the two would fight for the same pixels and the employee
	/// would look like a mess of flickering colours.
	private void hideCoveredBones(@NotNull T entity, @Nullable StaticEntityModel body) {
		if (body == null) {
			return;
		}

		/* if (entity.getUniformPiece(UniformShape.SHIRT) != null) {
			body.getTransform("torso").visible = false;
			body.getTransform("rightArm").visible = false;
			body.getTransform("leftArm").visible = false;
		} */

		if (entity.getUniformPiece(UniformShape.PANTS) != null) {
			body.getTransform("rightLeg").visible = false;
			body.getTransform("leftLeg").visible = false;
		}

		// Hide head only if hat doesn't overlap (i.e., hat covers hair)
		// If hat.overlaps() is true, hair can be visible under the hat
		/* if (entity.getUniformPiece(UniformShape.HAT) != null && !entity.getUniformPiece(UniformShape.HAT).overlaps()) {
			body.getTransform("head").visible = false;
		} */
	}

	private @Nullable StaticEntityModel setupUniformLayer(@NotNull T entity, float partialTick, int layer) {
		UniformShape slot = UNIFORM_LAYERS[layer - 1];
		UniformPiece piece = entity.getUniformPiece(slot);
		if (piece == null) return null;

		this.bindTexture(resolveUniformTexture(piece));
		StaticEntityModel model = this.getModel("uniform");
		if (model == null) return null;

		// Apply piece-specific visibility (open-extension point: new piece types can override)
		piece.applyUniformVisibility(model);

		StaticEntityModel result = this.setupAnimations(entity, model, partialTick, layer);
		return result;
	}

	private @Nullable StaticEntityModel setupHairLayer(@NotNull T entity, float partialTick) {
		UniformPiece hairPiece = entity.getUniformPiece(UniformShape.HAIR);
		if (hairPiece == null) return null;
		if (hairPiece.modelPath() == null) return null;

		this.bindTexture(resolveUniformTexture(hairPiece));
		StaticEntityModel model = this.getModel(hairPiece.modelPath());
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

		// Apply tint from piece data if available
		dbgOnce("hair-uniform:" + hairPiece.getTextureRef(), "hf-layer: uniform hair piece"
			+ " model=" + hairPiece.modelPath()
			+ " textureBasePath=" + hairPiece.textureBasePath());

		return model;
	}

	@Override
	protected @Nullable StaticEntityModel setupAnimations(@NonNull T entity, @Nullable StaticEntityModel model, float partialTick, int layer) {
		return super.setupAnimations(entity, model, partialTick, layer);
	}


	/// The bones of the uniform that belong to a piece: the jacket and the
	/// sleeves of the shirt, the pants of the trousers, the hat of the hat.
	///
	/// The other pieces of the uniform are hidden: each one is drawn in its
	/// own layer, and drawn again here they would pile up on the body (and
	/// the trousers would show a cube at the feet of the employee).
	private void applyUniformVisibility(@NotNull StaticEntityModel uniform, @NotNull UniformShape slot) {
		uniform.getTransform("jacket").visible = false;
		uniform.getTransform("rightSleeve").visible = false;
		uniform.getTransform("leftSleeve").visible = false;
		uniform.getTransform("hat").visible = false;
		uniform.getTransform("rightPant").visible = false;
		uniform.getTransform("leftPant").visible = false;

		switch (slot) {
			case SHIRT -> {
				uniform.getTransform("jacket").visible = true;
				uniform.getTransform("rightSleeve").visible = true;
				uniform.getTransform("leftSleeve").visible = true;
			}
			case PANTS -> {
				uniform.getTransform("rightPant").visible = true;
				uniform.getTransform("leftPant").visible = true;
			}
			case HAT -> {
				uniform.getTransform("hat").visible = true;
			}
			case GLASSES -> {
				// the glasses have no geometry yet
			}
		}
	}


	/**
	 * Resuelve la textura a usar para una pieza de uniforme.
	 * Usa la textura dinámica si está disponible, si no la por defecto.
	 * Consulta modelPath y textureBasePath/variantCount si la pieza los provee.
	 */
	protected String resolveUniformTexture(@NotNull UniformPiece piece) {
		// Priority: explicit textureBasePath + variant > textureRef > modelPath-based lookup
		if (piece.textureBasePath() != null && piece.variantCount() > 1) {
			int clamped = Math.max(0, Math.min(0, piece.variantCount() - 1)); // variant 0 by default
			return piece.textureBasePath() + clamped + ".png";
		}
		if (piece.modelPath() != null) {
			// TODO: implement model-path-based texture resolution
			return piece.modelPath();
		}
		// Fall back to the original texture reference
		return piece.getTextureRef();
	}

	@Override
	protected int maxRenderLayer(@NotNull T entity) {
		return HAIR_LAYER;
	}

	/// The hair tint is a global state of the GLRenderer (like the wool of the
	/// sheep), and nothing in the base loop restores it: after this entity is
	/// drawn, whatever the game draws next would come out with the colour of
	/// the hair (the "black box" bug). So the white is restored as soon as the
	/// render of the entity is done.
	@Override
	public void render(@NotNull TessellatorGeneral tessellator, @NotNull T entity,
					   double x, double y, double z, float entityYaw, float partialTick) {
		try {
			super.render(tessellator, entity, x, y, z, entityYaw, partialTick);
		} finally {
			GLRenderer.setColor3f(1.0f, 1.0f, 1.0f);
		}
	}
}
