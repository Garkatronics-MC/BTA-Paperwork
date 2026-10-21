package deus.paperwork.entity.employee.job.client;

import deus.paperwork.entity.employee.job.EmployeeModelSpec;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;

/// Applies an {@link EmployeeModelSpec} to a resolved model.
///
/// CLIENT ONLY: it touches Dragonfly classes, which the dedicated server does
/// not have. Common code (jobs, HUD) must never reference it; the renderer
/// calls it after the model has been set up, because the setup resets every
/// bone to visible.
public final class EmployeeModelApplier {

	private EmployeeModelApplier() {}

	/// Shows/hides the bones of the spec and applies their rotations.
	/// A null rotation means "leave the bone's rotation alone".
	public static void apply(StaticEntityModel model, EmployeeModelSpec spec) {
		if (model == null || spec == null) {
			return;
		}
		for (EmployeeModelSpec.EmployeeBone bone : spec.bones()) {
			BoneTransform transform = model.getTransform(bone.name());
			if (transform == null) {
				continue;
			}
			transform.visible = bone.visible();
			if (bone.pitch() != null) {
				transform.rotX = bone.pitch();
			}
			if (bone.yaw() != null) {
				transform.rotY = bone.yaw();
			}
			if (bone.roll() != null) {
				transform.rotZ = bone.roll();
			}
		}
	}
}
