package deus.paperwork.entities.employee.farmer;

import deus.paperwork.entities.employee.MobEmployee;
import deus.paperwork.entities.employee.job.client.EmployeeModelApplier;
import net.minecraft.client.render.entity.MobRendererBiped;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.useless.dragonfly.models.entity.StaticEntityModel;

/// The renderer of the deprecated farmer entity (the stub).
///
/// It resolves its model under the entity "paperwork:employee_farmer" (the
/// entry of models.json that T7 keeps) and applies the model spec of the
/// job, so the farmer shows the wheat it carries.
public class MobEmployeeFarmerRenderer extends MobRendererBiped<MobEmployee> {

	public MobEmployeeFarmerRenderer() {
		super(0.3f);
	}

	@Override
	protected @Nullable StaticEntityModel getActiveModel(@NonNull MobEmployee mobEmployee) {
		return this.getModel(mobEmployee.isWoman() ? "woman" : "man");
	}

	@Override
	protected @Nullable StaticEntityModel getAndSetupModelForLayer(
		@NonNull MobEmployee entity,
		float brightness,
		float partialTick,
		int layer
	) {
		if (entity.isWoman()) {
			this.bindTexture("/assets/paperwork/textures/entity/employee_farmer/woman/" + entity.getTextureReference() + ".png");
		} else {
			this.bindTexture("/assets/paperwork/textures/entity/employee_farmer/" + entity.getTextureReference() + ".png");
		}

		StaticEntityModel model = super.getAndSetupModelForLayer(
			entity,
			brightness,
			partialTick,
			layer
		);

		if (model == null) {
			return null;
		}

		// The setup of the model makes every bone visible again, so the spec of
		// the job (the wheat bones) is applied AFTER it, never before.
		EmployeeModelApplier.apply(model, entity.job().modelSpec(entity));

		return model;
	}
}
