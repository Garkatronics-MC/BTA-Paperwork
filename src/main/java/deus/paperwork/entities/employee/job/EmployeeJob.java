package deus.paperwork.entities.employee.job;

import deus.brainless.agent.Agent;
import deus.paperwork.entities.employee.MobEmployee;
import deus.paperwork.entities.employee.enums.EmployeeAlert;
import deus.paperwork.entities.employee.model.EmployeeStats;
import deus.paperwork.entities.employee.model.EmployeeStations;
import deus.paperwork.entities.employee.model.EmployeeTraits;

import java.util.Optional;

/// A job is a strategy: it owns both faces of a profession - the server logic
/// (extra station, recharge tick, own state, own alert) and the presentation
/// (model variant, texture folder, which bones are shown or posed).
///
/// Jobs are singletons without state: everything mutable lives in the mob or
/// in its {@link EmployeeJobState}. The id is a stable string ("plain",
/// "farmer") because it is persisted in NBT and synced to the client.
///
/// Common code must never reference client-only classes: the model is
/// applied by {@code EmployeeModelApplier} (client only) from the pure
/// {@link EmployeeModelSpec} this interface returns.
public interface EmployeeJob {

	/// Stable id of the job ("plain", "farmer"). Used in NBT and in synched data.
	String id();

	/// Model variant key inside models.json (the "paperwork:employee" entry) for that gender.
	String modelVariant(boolean isWoman);

	/// Texture folder under /assets/paperwork/textures/entity/ ("employee", "employee_farmer").
	String textureFolder();

	/// Presentation data of this job's model (bones to show/hide, poses).
	/// Computed from the mob; the job keeps no state of its own.
	EmployeeModelSpec modelSpec(MobEmployee mob);

	/// The brain of a mob with this job.
	Agent buildAgent(MobEmployee mob, EmployeeTraits traits, EmployeeStats stats, EmployeeStations stations);

	/// The stations of the job: the base ones (food/rest) plus the job's own (workplace).
	EmployeeStations stations(MobEmployee mob, EmployeeStations base);

	/// Job specific recharge; applied AFTER the base one (food/rest).
	void recharge(MobEmployee mob, EmployeeStats stats);

	/// Alert to show when the job's own station is missing; empty if the job has none.
	Optional<EmployeeAlert> missingStationAlert(MobEmployee mob);

	/// Clears the job's own stations (firing the employee).
	void clearStations(MobEmployee mob);

	/// Fresh (and empty) mutable state for a mob with this job.
	EmployeeJobState createState();
}
