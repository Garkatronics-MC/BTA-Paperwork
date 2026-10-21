package deus.paperwork.entity.employee.job;

import deus.brainless.agent.Agent;
import deus.paperwork.entity.employee.MobEmployee;
import deus.paperwork.entity.employee.MobEmployeeAgent;
import deus.paperwork.entity.employee.enums.EmployeeAlert;
import deus.paperwork.entity.employee.model.EmployeeStats;
import deus.paperwork.entity.employee.model.EmployeeStations;
import deus.paperwork.entity.employee.model.EmployeeTraits;

import java.util.List;
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

	/// The plain employee has no accessory: it wears what every employee wears.
	@Override
	public List<EmployeeAccessory> accessories() {
		return List.of();
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

	/// The plain employee has no work of its own: it eats, rests and wanders.
	@Override
	public void work(MobEmployee mob) {}

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
