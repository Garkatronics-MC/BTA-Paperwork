package deus.paperwork.entity.employee.chef;

import deus.brainless.agent.Agent;
import deus.paperwork.entity.employee.MobEmployee;
import deus.paperwork.entity.employee.enums.EmployeeAlert;
import deus.paperwork.entity.employee.job.EmployeeAccessory;
import deus.paperwork.entity.employee.job.EmployeeJob;
import deus.paperwork.entity.employee.job.EmployeeJobState;
import deus.paperwork.entity.employee.job.EmployeeJobs;
import deus.paperwork.entity.employee.job.EmployeeModelSpec;
import deus.paperwork.entity.employee.model.EmployeeStats;
import deus.paperwork.entity.employee.model.EmployeeStations;
import deus.paperwork.entity.employee.model.EmployeeTraits;
import deus.paperwork.core.util.PoscArea;

import java.util.List;
import java.util.Optional;

/// The chef job: an employee with a profession.
///
/// The job owns both faces of the profession:
/// - the server logic: the workplace area (min 3x6, max 16x16) where the
///   chef looks for furnace, workbench and chests; the recharge of standing
///   on the workplace; the mutable state that holds the area and the last
///   source/destination chests; and its own alert (NO_WORKPLACE) when the
///   area is missing;
/// - the presentation: no extra accessories or bones yet (the chef shares
///   the base employee look).
///
/// The job is a singleton without state: everything mutable lives in the mob,
/// in its {@link ChefJobState}.
public final class ChefJob implements EmployeeJob {

	public static final String CHEF_ID = "chef";

	/// How close to the workplace counts as standing on the job (drives the per-tick work recharge).
	private static final double WORK_NEAR_RANGE = 2.5;
	private static final double NEAR_WORK_RELIEF = 0.003;

	@Override
	public String id() {
		return CHEF_ID;
	}

	/// The chef has no accessory yet: it wears what every employee wears.
	@Override
	public List<EmployeeAccessory> accessories() {
		return List.of();
	}

	/// The base skeleton has no extra bones, so there is nothing to toggle.
	@Override
	public EmployeeModelSpec modelSpec(MobEmployee mob) {
		return EmployeeModelSpec.empty();
	}

	@Override
	public Agent buildAgent(MobEmployee mob, EmployeeTraits traits, EmployeeStats stats, EmployeeStations stations) {
		return ChefAgent.build(mob, traits, stats, stations);
	}

	/// The stations of a chef: the base ones (food, rest) plus the workplace as its work station.
	@Override
	public EmployeeStations stations(MobEmployee mob, EmployeeStations base) {
		return base.withWorkPlace(() -> workArea(mob).map(a -> (PoscArea) a));
	}

	/// Standing on the workplace pays the work deficit: that keeps the work drive
	/// alive so the cook plan continues.
	@Override
	public void recharge(MobEmployee mob, EmployeeStats stats) {
		Optional<PoscArea.Area2D> area = workArea(mob);
		if (area.isEmpty()) {
			return;
		}
		if (mob.near(area.get().center(), WORK_NEAR_RANGE)) {
			stats.restoreWork(NEAR_WORK_RELIEF);
		}
	}

	/// A chef without a workplace has no work station: that is its own alert.
	@Override
	public Optional<EmployeeAlert> missingStationAlert(MobEmployee mob) {
		return workArea(mob).isEmpty()
			? Optional.of(EmployeeAlert.NO_WORKPLACE)
			: Optional.empty();
	}

	/// Firing a chef takes its workplace away.
	@Override
	public void clearStations(MobEmployee mob) {
		if (mob != null && mob.jobState() instanceof ChefJobState chefState) {
			chefState.clearWorkArea();
			chefState.clearDestChest();
		}
	}

	/// The chef does its work through the cook task driven by the GOAP brain;
	/// there is nothing extra to do here on the per-tick work hook.
	@Override
	public void work(MobEmployee mob) {}

	/// Fresh (and empty) mutable state for a mob with this job.
	@Override
	public EmployeeJobState createState() {
		return new ChefJobState();
	}

	/// The workplace of a chef, read from the state of its job.
	///
	/// The state belongs to the mob and is of type {@link ChefJobState}; a
	/// mob whose state is another one (or the plain one) has no workplace at all.
	public static Optional<PoscArea.Area2D> workArea(MobEmployee mob) {
		if (mob == null) {
			return Optional.empty();
		}
		EmployeeJobState state = mob.jobState();
		if (state instanceof ChefJobState chefState) {
			return chefState.workArea();
		}
		return Optional.empty();
	}
}
