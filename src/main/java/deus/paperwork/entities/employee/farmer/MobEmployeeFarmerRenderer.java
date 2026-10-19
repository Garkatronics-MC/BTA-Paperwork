package deus.paperwork.entities.employee.farmer;

import deus.paperwork.entities.employee.MobEmployee;
import deus.paperwork.entities.employee.MobEmployeeRenderer;

/// The renderer of the deprecated farmer entity (the stub).
///
/// It is the generic renderer of the employee with ONE change: the stub is
/// registered as the entity "paperwork:employee_farmer", so its models live
/// under that id, whose keys are "man" and "woman" (not the ones of the
/// jobs). Everything else (the texture of the job, the bind, the model spec
/// of the job) is the generic one, in a single place.
public class MobEmployeeFarmerRenderer extends MobEmployeeRenderer {

	/// The models of the stub live under "paperwork:employee_farmer".
	@Override
	protected String modelVariant(MobEmployee employee) {
		return employee.isWoman() ? "woman" : "man";
	}
}
