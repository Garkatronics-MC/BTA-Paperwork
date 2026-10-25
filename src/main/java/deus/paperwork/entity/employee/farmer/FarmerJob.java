package deus.paperwork.entity.employee.farmer;

import deus.brainless.agent.Agent;
import deus.paperwork.entity.base.CropWorldAccess;
import deus.paperwork.entity.base.MobWorldAccess;
import deus.paperwork.entity.employee.MobEmployee;
import deus.paperwork.entity.employee.enums.EmployeeAlert;
import deus.paperwork.entity.employee.job.EmployeeAccessory;
import deus.paperwork.entity.employee.job.EmployeeJob;
import deus.paperwork.entity.employee.job.EmployeeJobState;
import deus.paperwork.entity.employee.job.EmployeeJobs;
import deus.paperwork.entity.employee.job.EmployeeModelSpec;
import deus.paperwork.entity.employee.job.EmployeeModelSpec.EmployeeBone;
import deus.paperwork.entity.employee.model.EmployeeStats;
import deus.paperwork.entity.employee.model.EmployeeStations;
import deus.paperwork.entity.employee.model.EmployeeTraits;
import deus.paperwork.core.util.PoscArea;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/// The farmer job: an employee with a profession.
///
/// The job owns both faces of the profession:
/// - the server logic: the plot it works (the workplace station), the
///   recharge of standing on the plot, the mutable state that holds the plot
///   and its own alert (NO_WORKPLACE) when the plot is missing;
/// - the presentation: the model variant (the farmer skeleton) and the bones
///   that show the wheat it carries.
///
/// The job is a singleton without state: everything mutable lives in the mob,
/// in its {@link FarmerJobState}.
public final class FarmerJob implements EmployeeJob {

	/// How close to the plot counts as standing on the job (drives the per-tick work recharge).
	private static final double WORK_NEAR_RANGE  = 2.5;
	private static final double NEAR_WORK_RELIEF = 0.003;

	/// How much wheat the farmer must carry for each wheat bone to show up.
	public static final int WHEAT_BONE_1 = 1;
	public static final int WHEAT_BONE_2 = 32;
	public static final int WHEAT_BONE_3 = 64;

	/// The bones of the farmer skeleton that show the wheat carried.
	private static final String BONE_WHEAT_1 = "wheat1";
	private static final String BONE_WHEAT_2 = "wheat2";
	private static final String BONE_WHEAT_3 = "wheat3";

	/// The bag of the farmer: the accessory layer that makes a farmer look like
	/// one. Its model is the one of the entity in models.json ("farmer_bag").
	private static final EmployeeAccessory FARMER_BAG = new EmployeeAccessory(
		"farmer_bag",
		"/assets/paperwork/textures/entity/employee/accessories/farmer_bag.png"
	);

	@Override
	public String id() {
		return EmployeeJobs.FARMER_ID;
	}

	/// The farmer carries its bag: an accessory layer on top of the employee,
	/// with the wheat it has gathered on it.
	@Override
	public List<EmployeeAccessory> accessories() {
		return List.of(FARMER_BAG);
	}

	/// The bones of the bag of the farmer: they show up as the farmer carries
	/// more wheat. The rest of the bones are left alone.
	@Override
	public EmployeeModelSpec modelSpec(MobEmployee mob) {
		return new EmployeeModelSpec(wheatBones(wheatCount(mob)));
	}

	/// The bones that show the wheat a farmer carries.
	///
	/// Pure and static: the amount of wheat is the only input, so this can be
	/// tested without a mob.
	///
	/// Every bone is always in the list, hidden when it does not apply: the
	/// setup of the model makes every bone visible again on every frame, so
	/// a bone that is not in the spec would pop up.
	public static List<EmployeeBone> wheatBones(int wheatCount) {
		List<EmployeeBone> bones = new ArrayList<>(3);
		bones.add(wheatBone(BONE_WHEAT_1, wheatCount >= WHEAT_BONE_1));
		bones.add(wheatBone(BONE_WHEAT_2, wheatCount >= WHEAT_BONE_2));
		bones.add(wheatBone(BONE_WHEAT_3, wheatCount >= WHEAT_BONE_3));
		return List.copyOf(bones);
	}

	private static EmployeeBone wheatBone(String name, boolean visible) {
		return visible ? EmployeeBone.shown(name) : EmployeeBone.hidden(name);
	}

	/// The wheat the farmer carries in its own inventory.
	///
	/// It is the first stack of wheat found, exactly as the old renderer read
	/// it: the refactor moves the rule, it does not change it.
	private static int wheatCount(MobEmployee mob) {
		if (mob == null) {
			return 0;
		}
		ItemStack stack = mob.inventory.findStackOf(Items.WHEAT);
		return stack == null ? 0 : stack.stackSize;
	}

	/// The plot of a farmer, read from the work zone of the mob.
	///
	/// The work zone is a base employee zone (assigned with the work-area
	/// stick), so every job reads the same place.
	public static Optional<PoscArea.Area2D> farmArea(MobEmployee mob) {
		if (mob == null) {
			return Optional.empty();
		}
		return mob.workArea;
	}

	// ------------------------------------------------------------------ the brain

	@Override
	public Agent buildAgent(MobEmployee mob, EmployeeTraits traits, EmployeeStats stats, EmployeeStations stations) {
		return FarmerAgent.build(mob, traits, stats, stations);
	}

	// ------------------------------------------------------------------ the stations

	/// The stations of a farmer: the base ones already include workArea from the
	/// mob; the farmer job just returns them as-is.
	@Override
	public EmployeeStations stations(MobEmployee mob, EmployeeStations base) {
		return base;
	}

	/// Standing on the plot pays the work deficit: that keeps the work drive
	/// alive so the plan continues until the plot is done.
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

	/// A farmer without a plot has no workplace: that is its own alert.
	@Override
	public Optional<EmployeeAlert> missingStationAlert(MobEmployee mob) {
		return mob.workArea.isEmpty()
			? Optional.of(EmployeeAlert.NO_WORKPLACE)
			: Optional.empty();
	}

	/// The farmer gathers: the wheat and the seeds on the floor around it, and
	/// the seeds of the chests next to it.
	@Override
	public void work(MobEmployee mob) {
		FarmerCollector.collect(mob);
	}

	/// Firing a farmer takes its plot away.
	@Override
	public void clearStations(MobEmployee mob) {
		if (mob != null) {
			mob.workArea = Optional.empty();
		}
	}

	/// The farmer needs farmland in its work area.
	@Override
	public String missingZoneItems(MobEmployee mob, PoscArea.Area2D workArea) {
		boolean hasFarmland = MobWorldAccess.getBlocksInArea3D(workArea).stream()
			.anyMatch(p -> mob.world.getBlockType(p).id() == Blocks.FARMLAND_DIRT.id());
		if (!hasFarmland) {
			return "no farmland in work zone";
		}
		return null;
	}

	/// There is work when something is ready to harvest, or when there is bare
	/// farmland and the farmer carries seeds to sow it.
	@Override
	public boolean hasWorkToDo(MobEmployee mob) {
		Optional<PoscArea.Area2D> area = mob.workArea;
		if (area.isEmpty()) {
			return false;
		}
		if (!CropWorldAccess.getHarvestableBlocksInArea(mob, area.get()).isEmpty()) {
			return true;
		}
		if (mob.inventory.findStackOf(Items.SEEDS_WHEAT) == null) {
			return false;
		}
		return !CropWorldAccess.getSowableBlocksInArea(mob, area.get()).isEmpty();
	}

	// ------------------------------------------------------------------ the state

	@Override
	public EmployeeJobState createState() {
		return new FarmerJobState();
	}
}
