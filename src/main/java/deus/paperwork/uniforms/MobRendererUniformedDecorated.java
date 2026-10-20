package deus.paperwork.uniforms;

import deus.paperwork.interfaces.ICustomizable;
import net.minecraft.client.render.entity.MobRendererBiped;
import net.minecraft.client.render.renderer.GLRenderer;
import net.minecraft.client.render.tessellator.TessellatorGeneral;import net.minecraft.core.entity.Mob;
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

	/// The hat has no art yet in the uniform textures (its region is black), so
	/// the head keeps its own face until there is one. When the hat is drawn in
	/// the textures, set this to true: the hat will then take the place of the
	/// head, as the rest of the uniform does with the body.
	private static final boolean HAT_HAS_ART = false;

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

		if (entity.getUniformPiece(UniformShape.SHIRT) != null) {
			body.getTransform("torso").visible = false;
			body.getTransform("rightArm").visible = false;
			body.getTransform("leftArm").visible = false;
		}

		if (entity.getUniformPiece(UniformShape.PANTS) != null) {
			body.getTransform("rightLeg").visible = false;
			body.getTransform("leftLeg").visible = false;
		}

		if (HAT_HAS_ART && entity.getUniformPiece(UniformShape.HAT) != null) {
			body.getTransform("head").visible = false;
		}
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
				uniform.getTransform("hat").visible = HAT_HAS_ART;
			}
			case GLASSES -> {
				// the glasses have no geometry yet
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
