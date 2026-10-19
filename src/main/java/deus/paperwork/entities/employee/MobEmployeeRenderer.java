package deus.paperwork.entities.employee;

import deus.paperwork.entities.employee.job.EmployeeAccessory;
import deus.paperwork.entities.employee.job.EmployeeModelSpec;
import deus.paperwork.entities.employee.job.client.EmployeeModelApplier;
import deus.paperwork.uniforms.MobRendererUniformedDecorated;
import net.minecraft.client.render.entity.MobRendererBiped;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.useless.dragonfly.models.entity.StaticEntityModel;

import java.util.List;

/// The one renderer of every employee: the model, the texture and the layers
/// of an employee come from its job, so the renderer never mentions a
/// profession.
///
/// The employee is drawn in layers:
/// - layer 0: the body of the employee (the same for every employee);
/// - layers 1 to 5: the uniform pieces and the hair (the look of the employee);
/// - the layers after that: the accessories of its job (the bag of the farmer).
///
/// The job decides the accessories and the bones of its own models (the wheat
/// the farmer carries); the body, the uniform and the hair belong to the
/// employee itself.
public class MobEmployeeRenderer extends MobRendererUniformedDecorated<MobEmployee> {

	/// The model of the body of every employee.
	private static final String BODY_MODEL = "main";

	/// The layer of the hair. The accessories of the job come after it.
	private static final int ACCESSORY_LAYER = HAIR_LAYER + 1;

	/// The root of the textures of the entities.
	private static final String TEXTURE_ROOT = "/assets/paperwork/textures/entity/";

	public MobEmployeeRenderer() {
		super(0.3f);
	}

	@Override
	protected @Nullable StaticEntityModel getActiveModel(@NonNull MobEmployee mobEmployee) {
		return this.getModel(BODY_MODEL);
	}

	@Override
	protected @Nullable StaticEntityModel getAndSetupModelForLayer(
		@NonNull MobEmployee entity,
		float brightness,
		float partialTick,
		int layer
	) {
		List<EmployeeAccessory> accessories = entity.job().accessories();
		int accessoryIndex = layer - ACCESSORY_LAYER;

		if (accessoryIndex < 0 || accessoryIndex >= accessories.size()) {
			// The body, the uniform and the hair: the base renderer knows them.
			if (layer == 0) {
				this.bindTexture(texturePath(entity));
			}
			return super.getAndSetupModelForLayer(entity, brightness, partialTick, layer);
		}

		EmployeeAccessory accessory = accessories.get(accessoryIndex);
		this.bindTexture(accessory.texture());

		StaticEntityModel model = this.getModel(accessory.model());
		if (model == null) {
			return null;
		}

		StaticEntityModel result = this.setupAnimations(entity, model, partialTick, layer);

		// ALWAYS after the setup: the setup of the model resets every bone to
		// visible, so the toggles of the job would be erased on every frame.
		EmployeeModelApplier.apply(model, entity.job().modelSpec(entity));

		return result;
	}

	/// The texture of the body of an employee. Every employee wears the same
	/// ones now: the job adds layers on top, not a texture folder.
	private static String texturePath(MobEmployee entity) {
		return TEXTURE_ROOT + entity.getTextureReference() + ".png";
	}

	/// How many layers are drawn: the body, the uniform, the hair and the
	/// accessories of the job of this employee.
	@Override
	protected int maxRenderLayer(@NonNull MobEmployee entity) {
		return ACCESSORY_LAYER + entity.job().accessories().size() - 1;
	}
}
