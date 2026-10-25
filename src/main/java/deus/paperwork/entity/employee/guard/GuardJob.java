package deus.paperwork.entity.employee.guard;

import deus.brainless.agent.Agent;
import deus.paperwork.core.util.PoscArea;
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
import net.minecraft.core.entity.Entity;

import java.util.List;
import java.util.Optional;

/// The guard job: an employee that attacks the hostile mobs around it.
///
/// The job owns the target of the fight. Every tick it looks for the nearest
/// {@link net.minecraft.core.entity.monster.Enemy} (or keeps the one it is
/// already chasing) and hands it to the mob, which is where the Brainless
/// fight tasks read it from. The {@link GuardAgent} is what turns that target
/// into a fight: approach it, strike it, and fall back to the normal employee
/// life when there is nothing hostile nearby.
///
/// The job is a singleton without state: everything mutable lives in the mob.
public final class GuardJob implements EmployeeJob {

	/// Stable id of the job ("guard"), persisted in NBT and synced to the client.
	public static final String GUARD_ID = EmployeeJobs.GUARD_ID;

	@Override
	public String id() {
		return EmployeeJobs.GUARD_ID;
	}

	/// The guard has no accessory yet: it wears what every employee wears.
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
		return GuardAgent.build(mob, traits, stats, stations);
	}

	/// The stations of a guard: the base ones already include workArea from the
	/// mob; the guard job just returns them as-is.
	@Override
	public EmployeeStations stations(MobEmployee mob, EmployeeStations base) {
		return base;
	}

	/// The guard does not need a work station to do its job: it fights what it
	/// finds, wherever it is. There is nothing extra to recharge either.
	@Override
	public void recharge(MobEmployee mob, EmployeeStats stats) {}

	/// Picks (or keeps) the hostile target of the guard before the brain runs.
	///
	/// This is the only piece of the job that needs the world, and it is also
	/// the one that makes the fight possible: the Brainless tasks only know how
	/// to attack {@code mob.getTarget()}, they never go looking for it.
	@Override
	public void sense(MobEmployee mob, Agent agent) {
		if (mob == null || agent == null) {
			return;
		}

		Entity target = mob.getTarget();
		if (!GuardTargeting.canKeepTarget(mob, target)) {
			target = GuardTargeting.findNearestHostile(mob).orElse(null);
			mob.setTarget(target);
		}

		agent.sense(GuardAgent.GUARD_THREAT_INPUT, target == null ? 0.0 : 1.0);
	}

	/// The fight happens through the brain; the per-tick work hook has nothing
	/// else to do.
	@Override
	public void work(MobEmployee mob) {}

	/// A guard is useful without a workplace: it never asks for one.
	@Override
	public Optional<EmployeeAlert> missingStationAlert(MobEmployee mob) {
		return Optional.empty();
	}

	/// The guard owns no station of its own (the base zones are cleared by the
	/// mob), so there is nothing extra to clear.
	@Override
	public void clearStations(MobEmployee mob) {}

	/// The guard does not need anything specific in its work area.
	@Override
	public String missingZoneItems(MobEmployee mob, PoscArea.Area2D workArea) {
		return null;
	}

	/// There is work while the guard has a hostile mob to fight.
	@Override
	public boolean hasWorkToDo(MobEmployee mob) {
		return mob != null && GuardTargeting.canKeepTarget(mob, mob.getTarget());
	}

	@Override
	public EmployeeJobState createState() {
		return new EmployeeJobState();
	}
}
