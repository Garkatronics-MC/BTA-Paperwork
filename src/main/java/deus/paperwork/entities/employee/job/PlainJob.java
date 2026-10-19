package deus.paperwork.entities.employee.job;

import deus.brainless.agent.Agent;
import deus.paperwork.entities.employee.MobEmployee;
import deus.paperwork.entities.employee.MobEmployeeAgent;
import deus.paperwork.entities.employee.enums.EmployeeAlert;
import deus.paperwork.entities.employee.model.EmployeeStats;
import deus.paperwork.entities.employee.model.EmployeeStations;
import deus.paperwork.entities.employee.model.EmployeeTraits;

import java.util.Optional;

/// The generic employee: no profession, no workplace, just the base life
/// (eat, rest, wander, fight).
///
/// It is the job every employee starts with, and the only one the base knows
/// how to build.
public final class PlainJob implements EmployeeJob {

	@Override
	public String id() {
		return EmployeeJobs.PLAIN_ID;
	}

	@Override
	public String modelVariant(boolean isWoman) {
		return isWoman ? "woman" : "man";
	}

	@Override
	public String textureFolder() {
		return "employee";
	}

	/// The plain skeleton has no extra bones, so there is nothing to toggle.
	@Override
	public EmployeeModelSpec modelSpec(MobEmployee mob) {
		return EmployeeModelSpec.empty();
	}

	@Override
	public Agent buildAgent(MobEmployee mob, EmployeeTraits traits, EmployeeStats stats, EmployeeStations stations) {
		return MobEmployeeAgent.build(mob, traits, stats, stations);
	}

	/// The plain job adds no station: the base ones (food/rest) are enough.
	@Override
	public EmployeeStations stations(MobEmployee mob, EmployeeStations base) {
		return base;
	}

	/// The base recharge (food/rest) already lives in MobEmployee.
	@Override
	public void recharge(MobEmployee mob, EmployeeStats stats) {}

	/// The plain job has no station of its own, so it never asks for one.
	@Override
	public Optional<EmployeeAlert> missingStationAlert(MobEmployee mob) {
		return Optional.empty();
	}

	/// The plain job owns no station, so there is nothing to clear.
	@Override
	public void clearStations(MobEmployee mob) {}

	@Override
	public EmployeeJobState createState() {
		return new EmployeeJobState();
	}
}
