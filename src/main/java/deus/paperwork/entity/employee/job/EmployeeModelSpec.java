package deus.paperwork.entity.employee.job;

import java.util.List;

/// The presentation data of an employee's model: which bones are shown or
/// hidden and how some of them are posed.
///
/// It is pure data (no Minecraft classes), so the job can compute it on
/// either side and the client-only applier can turn it into bone changes.
public record EmployeeModelSpec(List<EmployeeBone> bones) {

	/// Defensive copy: the spec owns an immutable list of bones.
	public EmployeeModelSpec {
		bones = List.copyOf(bones);
	}

	/// Nothing to change: every bone of the skeleton stays as it is.
	public static EmployeeModelSpec empty() {
		return new EmployeeModelSpec(List.of());
	}

	/// One bone of the skeleton.
	///
	/// The poses (pitch/yaw/roll, set with {@link #posed}) are not used by any
	/// job yet: they stay because the spec defines the presentation of a job
	/// as "the setup of visuals/poses" and mentions "the poses of the job"
	/// twice. Today the farmer only turns bones on and off, but the mechanism
	/// belongs to the spec, not to the farmer.
	///
	/// @param name    bone name, as in the geometry
	/// @param visible whether the bone is drawn
	/// @param pitch   rotation on X, null means "do not touch the rotation"
	/// @param yaw     rotation on Y, null means "do not touch the rotation"
	/// @param roll    rotation on Z, null means "do not touch the rotation"
	public record EmployeeBone(String name, boolean visible, Double pitch, Double yaw, Double roll) {

		/// A bone that must not be drawn.
		public static EmployeeBone hidden(String name) {
			return new EmployeeBone(name, false, null, null, null);
		}

		/// A bone that must be drawn, with its rotation untouched.
		public static EmployeeBone shown(String name) {
			return new EmployeeBone(name, true, null, null, null);
		}

		/// A visible bone with a rotation of its own.
		public static EmployeeBone posed(String name, double pitch, double yaw, double roll) {
			return new EmployeeBone(name, true, pitch, yaw, roll);
		}
	}
}
