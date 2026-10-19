package deus.paperwork.entities.employee;

import deus.paperwork.entities.employee.job.client.EmployeeModelApplier;
import net.minecraft.client.render.entity.MobRendererBiped;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.useless.dragonfly.models.entity.StaticEntityModel;

/// The one renderer of every employee: the model and the texture of an
/// employee come from its job, so the renderer never mentions a profession.
///
/// The job decides the model variant (the key inside the "paperwork:employee"
/// entry of models.json) and the texture folder; the bones of the model (the
/// wheat of a job, ...) come from the model spec of the job.
public class MobEmployeeRenderer extends MobRendererBiped<MobEmployee> {

	/// The root of the textures of the entities.
	private static final String TEXTURE_ROOT = "/assets/paperwork/textures/entity/";

	/// The subfolder of the female skins. It is a convention of the textures,
	/// not of the job.
	private static final String WOMAN_FOLDER = "woman/";

	public MobEmployeeRenderer() {
		super(0.3f);
	}

	/// The model of the employee: the variant of its job for its gender.
	@Override
	protected @Nullable StaticEntityModel getActiveModel(@NonNull MobEmployee mobEmployee) {
		return this.getModel(mobEmployee.job().modelVariant(mobEmployee.isWoman()));
	}

	@Override
	protected @Nullable StaticEntityModel getAndSetupModelForLayer(@NonNull MobEmployee entity, float brightness, float partialTick, int layer) {
		this.bindTexture(texturePath(entity));

		StaticEntityModel model = super.getAndSetupModelForLayer(entity, brightness, partialTick, layer);
		if (model == null) {
			return null;
		}

		// ALWAYS after the super call: the setup of the model resets every
		// bone to visible, so the toggles of the job would be erased on
		// every frame.
		EmployeeModelApplier.apply(model, entity.job().modelSpec(entity));

		return model;
	}

	/// The texture of an employee: the folder of its job, the subfolder of
	/// its gender and the skin reference of the mob.
	private static String texturePath(MobEmployee entity) {
		return TEXTURE_ROOT
			+ entity.job().textureFolder() + "/"
			+ (entity.isWoman() ? WOMAN_FOLDER : "")
			+ entity.getTextureReference() + ".png";
	}
}
