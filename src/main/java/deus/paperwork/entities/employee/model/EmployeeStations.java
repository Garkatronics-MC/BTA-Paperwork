package deus.paperwork.entities.employee.model;

import deus.paperwork.ai.tasks.Food;
import deus.paperwork.util.PoscArea;

import java.util.Optional;
import java.util.function.Supplier;

/// The world anchors an employee stands on: where it eats, how it eats,
/// where it rests, where it works. Suppliers so the brain can be built
/// before the entity has been assigned its places (a stick click, chest
/// placement, ...).
///
/// The owner of the entity composes this record and hands it to
/// {@code MobEmployeeAgent.build(...)}.
public record EmployeeStations(
	Supplier<Optional<? extends PoscArea>> foodPlace,
	Supplier<Optional<Food>> foodSource,
	Supplier<Optional<? extends PoscArea>> restPlace,
	Supplier<Optional<? extends PoscArea>> workPlace
) {

	/// An employee with no places assigned: every station task fails fast,
	/// keeping only the built-in behaviour (wander, fight) alive.
	public static EmployeeStations none() {
		return new EmployeeStations(
			Optional::empty,
			Optional::empty,
			Optional::empty,
			Optional::empty
		);
	}

	/// A composed employee that lies down for a second role: everything stays,
	/// the workplace is swapped in.
	public EmployeeStations withWorkPlace(Supplier<Optional<? extends PoscArea>> workPlace) {
		return new EmployeeStations(foodPlace, foodSource, restPlace, workPlace);
	}
}
