package deus.paperwork.entities.employee.farmer;

import deus.paperwork.entities.employee.MobEmployee;
import deus.paperwork.entities.employee.job.EmployeeJobs;
import deus.utils.annotations.RegisterEntity;
import deus.utils.annotations.RegisterEntityRenderer;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;

/// The farmer as an entity: DEPRECATED, kept only for the old saves.
///
/// The farmer is a job now ({@link FarmerJob}), not a class of mob: any
/// employee can become a farmer at runtime. This stub stays registered as the
/// entity "employee_farmer" so the employees saved with it keep spawning, and
/// its only job is to force the farmer profession on them.
///
/// It has no logic of its own: the brain, the stations, the recharge, the
/// plot and the alert belong to the job.
@RegisterEntityRenderer(renderer = MobEmployeeFarmerRenderer.class)
@RegisterEntity(id = "employee_farmer", name = "employee_farmer")
@Deprecated
public class MobEmployeeFarmer extends MobEmployee {

	public MobEmployeeFarmer(@NotNull World world) {
		super(world);
		// The job is forced HERE, and not in readAdditionalSaveData: the client
		// does not run the NBT when it creates the entity, so the job would
		// only reach it with the synced data, and the farmer would be drawn as
		// a plain employee until then.
		setJob(EmployeeJobs.FARMER_ID);
	}
}
