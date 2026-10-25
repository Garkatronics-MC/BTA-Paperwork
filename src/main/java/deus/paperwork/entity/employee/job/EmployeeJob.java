package deus.paperwork.entity.employee.job;

import deus.brainless.agent.Agent;
import deus.paperwork.entity.employee.MobEmployee;
import deus.paperwork.entity.employee.enums.EmployeeAlert;
import deus.paperwork.entity.employee.model.EmployeeStats;
import deus.paperwork.entity.employee.model.EmployeeStations;
import deus.paperwork.core.util.PoscArea;
import deus.paperwork.entity.employee.model.EmployeeTraits;

import java.util.List;
import java.util.Optional;

/// A job is a strategy: it owns both faces of a profession - the server logic
/// (extra station, recharge tick, own state, own alert) and the presentation
/// (the accessory layers it adds, which bones are shown or posed).
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

	/// The accessory layers this job adds on top of the employee: its own
	/// models (the bag of the farmer, ...) with the texture of each one.
	///
	/// Every employee shares the same base model, the same uniform and the same
	/// hair; the job is what makes it look like its profession.
	List<EmployeeAccessory> accessories();

	/// Presentation data of this job's own models (bones to show/hide, poses).
	/// Computed from the mob; the job keeps no state of its own.
	EmployeeModelSpec modelSpec(MobEmployee mob);

	/// The brain of a mob with this job.
	///
	/// The signature repeats data the mob already has (the traits and the
	/// stats): it stays because the spec fixes that exact signature for the
	/// recharge ("the strategy needs both the stats and the distance test")
	/// and the same reasoning applies to the agent.
	Agent buildAgent(MobEmployee mob, EmployeeTraits traits, EmployeeStats stats, EmployeeStations stations);

	/// The stations of the job: the base ones (food/rest) plus the job's own (workplace).
	EmployeeStations stations(MobEmployee mob, EmployeeStations base);

	/// Job specific recharge; applied AFTER the base one (food/rest).
	void recharge(MobEmployee mob, EmployeeStats stats);

	/// What the employee does with the world because of its profession. It runs
	/// on the server, every tick.
	///
	/// The farmer gathers what is around it here; the plain employee has
	/// nothing to do.
	void work(MobEmployee mob);

	/// Alert to show when the job's own station is missing; empty if the job has none.
	Optional<EmployeeAlert> missingStationAlert(MobEmployee mob);

	/// Clears the job's own stations (firing the employee).
	void clearStations(MobEmployee mob);

	/// Describes what is missing in the given work area for this job, or null
	/// when the area has everything the job needs. Used for the per-minute
	/// excuse messages.
	String missingZoneItems(MobEmployee mob, PoscArea.Area2D workArea);

	/// Whether this job has real work available right now. While the employee
	/// idles it keeps wandering, but as soon as this turns true the stroll is
	/// cut short so the work drive can take over.
	///
	/// It must stay cheap enough to be polled, and it must never have side
	/// effects: it only looks at the world.
	default boolean hasWorkToDo(MobEmployee mob) {
		return false;
	}

	/// Fresh (and empty) mutable state for a mob with this job.
	EmployeeJobState createState();
}
