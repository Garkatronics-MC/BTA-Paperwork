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
import deus.paperwork.entity.base.MobWorldAccess;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.world.pos.TilePosc;

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

	/// The stations of a chef: the base ones already include workArea from the
	/// mob; the chef job just returns them as-is.
	@Override
	public EmployeeStations stations(MobEmployee mob, EmployeeStations base) {
		return base;
	}

	/// Standing on the workplace pays the work deficit: that keeps the work drive
	/// alive so the cook plan continues.
	@Override
	public void recharge(MobEmployee mob, EmployeeStats stats) {
		Optional<PoscArea.Area2D> area = mob.workArea;
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
		return mob.workArea.isEmpty()
			? Optional.of(EmployeeAlert.NO_WORKPLACE)
			: Optional.empty();
	}

	/// Firing a chef takes its workplace away.
	@Override
	public void clearStations(MobEmployee mob) {
		if (mob != null) {
			mob.workArea = Optional.empty();
			mob.homeArea = Optional.empty();
			mob.restArea = Optional.empty();
			mob.foodArea = Optional.empty();
		}
		if (mob != null && mob.jobState() instanceof ChefJobState chefState) {
			chefState.clearDestChest();
		}
	}

	/// The chef needs a furnace or workbench and at least one chest in its work area.
	@Override
	public String missingZoneItems(MobEmployee mob, PoscArea.Area2D workArea) {
		boolean hasCookStation = false;
		boolean hasChest = false;
		for (TilePosc p : MobWorldAccess.getBlocksInArea3D(workArea)) {
			int id = mob.world.getBlockType(p).id();
			if (id == Blocks.FURNACE_STONE_IDLE.id()
				|| id == Blocks.FURNACE_STONE_ACTIVE.id()
				|| id == Blocks.WORKBENCH.id()) {
				hasCookStation = true;
			}
			if (MobWorldAccess.getChest(mob.world, p) != null) {
				hasChest = true;
			}
			if (hasCookStation && hasChest) break;
		}
		StringBuilder sb = new StringBuilder();
		if (!hasCookStation) {
			sb.append("no furnace or workbench in work zone");
		}
		if (!hasChest) {
			if (sb.length() > 0) sb.append("; ");
			sb.append("no chest in work zone");
		}
		return sb.length() > 0 ? sb.toString() : null;
	}

	/// The chef does its work through the cook task driven by the GOAP brain;
	/// there is nothing extra to do here on the per-tick work hook.
	@Override
	public void work(MobEmployee mob) {}

	/// There is work when the chef carries something smeltable or a chest in
	/// the kitchen holds it.
	@Override
	public boolean hasWorkToDo(MobEmployee mob) {
		return mob.workArea.map(area -> CookTask.hasCookableWork(mob, area)).orElse(false);
	}

	/// Fresh (and empty) mutable state for a mob with this job.
	@Override
	public EmployeeJobState createState() {
		return new ChefJobState();
	}

	/// The workplace of a chef, read from the work zone of the mob.
	///
	/// The work zone is a base employee zone (assigned with the work-area
	/// stick), so every job reads the same place.
	public static Optional<PoscArea.Area2D> workArea(MobEmployee mob) {
		if (mob == null) {
			return Optional.empty();
		}
		return mob.workArea;
	}
}
