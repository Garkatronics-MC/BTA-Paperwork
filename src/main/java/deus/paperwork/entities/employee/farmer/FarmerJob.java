package deus.paperwork.entities.employee.farmer;

import deus.brainless.agent.Agent;
import deus.paperwork.entities.employee.MobEmployee;
import deus.paperwork.entities.employee.enums.EmployeeAlert;
import deus.paperwork.entities.employee.job.EmployeeJob;
import deus.paperwork.entities.employee.job.EmployeeJobState;
import deus.paperwork.entities.employee.job.EmployeeJobs;
import deus.paperwork.entities.employee.job.EmployeeModelSpec;
import deus.paperwork.entities.employee.job.EmployeeModelSpec.EmployeeBone;
import deus.paperwork.entities.employee.model.EmployeeStats;
import deus.paperwork.entities.employee.model.EmployeeStations;
import deus.paperwork.entities.employee.model.EmployeeTraits;
import deus.paperwork.util.PoscArea;
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

	@Override
	public String id() {
		return EmployeeJobs.FARMER_ID;
	}

	@Override
	public String modelVariant(boolean isWoman) {
		return isWoman ? "farmer_woman" : "farmer_man";
	}

	@Override
	public String textureFolder() {
		return "employee_farmer";
	}

	/// The farmer skeleton has three wheat bones: they show up as the farmer
	/// carries more wheat. The rest of the bones (hat, seed_bag, wheat_bag)
	/// are left alone.
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

	/// The plot of a farmer, read from the state of its job.
	///
	/// The state belongs to the mob and is of type {@link FarmerJobState}; a
	/// mob whose state is another one (or the plain one) has no plot at all.
	public static Optional<PoscArea.Area2D> farmArea(MobEmployee mob) {
		if (mob == null) {
			return Optional.empty();
		}
		EmployeeJobState state = mob.jobState();
		if (state instanceof FarmerJobState farmerState) {
			return farmerState.farmArea();
		}
		return Optional.empty();
	}

	// ------------------------------------------------------------------ the brain

	@Override
	public Agent buildAgent(MobEmployee mob, EmployeeTraits traits, EmployeeStats stats, EmployeeStations stations) {
		return FarmerAgent.build(mob, traits, stats, stations);
	}

	// ------------------------------------------------------------------ the stations

	/// The stations of a farmer: the base ones (food, rest) plus the plot as
	/// its workplace.
	@Override
	public EmployeeStations stations(MobEmployee mob, EmployeeStations base) {
		return base.withWorkPlace(() -> farmArea(mob).map(a -> (PoscArea) a));
	}

	/// Standing on the plot pays the work deficit: that keeps the work drive
	/// alive so the plan continues until the plot is done.
	@Override
	public void recharge(MobEmployee mob, EmployeeStats stats) {
		Optional<PoscArea.Area2D> area = farmArea(mob);
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
		return farmArea(mob).isEmpty()
			? Optional.of(EmployeeAlert.NO_WORKPLACE)
			: Optional.empty();
	}

	/// Firing a farmer takes its plot away.
	@Override
	public void clearStations(MobEmployee mob) {
		if (mob != null && mob.jobState() instanceof FarmerJobState farmerState) {
			farmerState.clearFarmArea();
		}
	}

	// ------------------------------------------------------------------ the state

	@Override
	public EmployeeJobState createState() {
		return new FarmerJobState();
	}
}
