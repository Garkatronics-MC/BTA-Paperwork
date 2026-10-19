package deus.paperwork.entities.employee.farmer;

import deus.brainless.agent.Agent;
import deus.paperwork.entities.employee.MobEmployee;
import deus.paperwork.entities.employee.model.EmployeeStations;
import deus.paperwork.util.PoscArea;
import deus.utils.annotations.RegisterEntity;
import deus.utils.annotations.RegisterEntityRenderer;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@RegisterEntityRenderer(renderer = MobEmployeeFarmerRenderer.class)
@RegisterEntity(id = "employee_farmer", name = "employee_farmer")
public class MobEmployeeFarmer extends MobEmployee {

	/// How close to the farm area counts as standing on the job (drives the per-tick work recharge).
	private static final double WORK_NEAR_RANGE  = 2.5;
	private static final double NEAR_WORK_RELIEF = 0.003;

	/// Assigned with the FarmStick: the field the farmer works.
	public Optional<PoscArea.Area2D> farm_area = Optional.empty();

	public MobEmployeeFarmer(@NotNull World world) {
		super(world);
		// The farmer wears the same model as any employee now: the bag is an
		// accessory layer of the renderer, not a texture folder of its own.
		//this.setTextureIdentifier("paperwork", "employee_farmer");
	}

	// ------------------------------------------------------------------ the brain

	@Override
	protected Agent createAgent() {
		return EmployeeFarmerAgent.build(this, traits(), stats(), stations());
	}

	/// Same stations as any employee (food, rest), plus the farm as workplace.
	@Override
	public EmployeeStations stations() {
		return super.stations().withWorkPlace(() -> farm_area.map(a -> (PoscArea) a));
	}

	@Override
	protected void updateAI() {
		super.updateAI();   // HUD, station recharges, brain loop (server-side guarded)

		if (this.world.isClientSide || farm_area.isEmpty()) return;
		// standing on the job pays the work deficit; that keeps the work drive
		// alive so the plan continues until the area is done.
		if (near(farm_area.get().center(), WORK_NEAR_RANGE)) {
			stats().restoreWork(NEAR_WORK_RELIEF);
		}
	}

	// ------------------------------------------------------------------ farming

	@Override
	public void clearStations() {
		super.clearStations();
		farm_area = Optional.empty();
	}
}
