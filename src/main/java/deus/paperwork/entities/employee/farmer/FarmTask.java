package deus.paperwork.entities.employee.farmer;

import deus.brainless.task.StopReason;
import deus.brainless.task.Task;
import deus.brainless.task.TaskStatus;
import deus.paperwork.Paperwork;
import deus.paperwork.entities.base.CropWorldAccess;
import deus.paperwork.entities.employee.MobEmployee;
import deus.paperwork.util.PoscArea;
import net.minecraft.core.world.pos.TilePos;
import net.minecraft.core.world.pos.TilePosc;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/// Works the farm area: harvests ready crops first, then sows empty tiles.
/// Succeeds once the farm has nothing left to do; fails when no farm area is set.
///
/// The area is not a field of the mob anymore: it lives in the state of the
/// job, and the task reads it through the job itself
/// ({@link FarmerJob#farmArea(MobEmployee)}), so it does not know any concrete
/// farmer class.
public class FarmTask implements Task {

	private static final double ACTION_REACH = 1.2;

	private final MobEmployee mob;

	private TilePos currentTarget = null;
	private boolean isHarvestAction = false;

	public FarmTask(MobEmployee mob) {
		this.mob = mob;
	}

	@Override
	public void start() {
		currentTarget = null;
		isHarvestAction = false;
	}

	@Override
	public TaskStatus tick() {
		Optional<PoscArea.Area2D> farmAreaOpt = farmArea();
		if (farmAreaOpt.isEmpty()) {
			Paperwork.LOGGER.info("[Farm] farm area is empty, nothing to do");
			return TaskStatus.FAILURE;
		}
		PoscArea.Area2D area = farmAreaOpt.get();

		// Already has a target — keep moving toward it
		if (currentTarget != null) {
			mob.navigateTo(currentTarget.x(), currentTarget.y(), currentTarget.z());

			if (mob.near(currentTarget, ACTION_REACH)) {
				if (isHarvestAction) {
					TilePos crop = new TilePos(currentTarget.x(), currentTarget.y(), currentTarget.z());
					boolean harvested = CropWorldAccess.harvest(mob, crop);
					Paperwork.LOGGER.info("[Farm] Harvest at {} -> {}", crop, harvested ? "OK" : "FAILED");
				} else {
					TilePos soil = new TilePos(currentTarget.x(), currentTarget.y(), currentTarget.z());
					boolean seeded = CropWorldAccess.seed(mob, mob.inventory, soil);
					Paperwork.LOGGER.info("[Farm] Seed at {} -> {}", soil, seeded ? "OK" : "FAILED");
				}
				currentTarget = null;
				Paperwork.LOGGER.info("[Farm] Target cleared, next tick will scan");
			}
			return TaskStatus.RUNNING;
		}

		// Scan harvestable
		List<TilePosc> harvestable = CropWorldAccess.getHarvestableBlocksInArea(mob, area).stream()
			.sorted(Comparator.comparingInt(TilePosc::z).thenComparingInt(TilePosc::x))
			.toList();

		if (!harvestable.isEmpty()) {
			TilePosc ground = harvestable.get(0);
			currentTarget = new TilePos(ground.x(), ground.y() + 1, ground.z());
			isHarvestAction = true;
			mob.navigateTo(currentTarget.x(), currentTarget.y(), currentTarget.z());
			Paperwork.LOGGER.info("[Farm] Harvest target set: {} ({} crops ready)", currentTarget, harvestable.size());
			return TaskStatus.RUNNING;
		}

		// Scan sowable
		List<TilePosc> sowable = CropWorldAccess.getSowableBlocksInArea(mob, area).stream()
			.sorted(Comparator.comparingInt(TilePosc::z).thenComparingInt(TilePosc::x))
			.toList();

		if (!sowable.isEmpty()) {
			TilePosc ground = sowable.get(0);
			currentTarget = new TilePos(ground.x(), ground.y() + 1, ground.z());
			isHarvestAction = false;
			mob.navigateTo(currentTarget.x(), currentTarget.y(), currentTarget.z());
			Paperwork.LOGGER.info("[Farm] Sow target set: {} ({} tiles available)", currentTarget, sowable.size());
			return TaskStatus.RUNNING;
		}

		// Nothing to do — farm is idle
		Paperwork.LOGGER.info("[Farm] No harvestable or sowable tiles found in area");
		return TaskStatus.SUCCESS;
	}

	@Override
	public void stop(StopReason reason) {
		Paperwork.LOGGER.info("[Farm] stop ({}) — clearing target", reason);
		currentTarget = null;
		mob.stopMoving();
	}

	/// The plot the farmer works, read from the state of its job.
	///
	/// The job knows how to read its own state, so the task does not have to
	/// know the concrete type of the job state.
	private Optional<PoscArea.Area2D> farmArea() {
		return FarmerJob.farmArea(mob);
	}
}
