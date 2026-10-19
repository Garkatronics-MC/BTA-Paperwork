package deus.paperwork.entities.employee.model;

import deus.paperwork.entities.employee.enums.EmployeeAlert;
import deus.paperwork.entities.employee.enums.EmployeeEmotions;
import deus.paperwork.entities.employee.enums.EmployeeStateIcons;

/// The HUD state of an employee: the alert icon, the state icon, the emotion
/// and the work progress bar drawn over its head.
///
/// It is pure: the server computes it and publishes it, the client only
/// draws it. The job's own alert (the farmer's missing workplace) enters
/// through {@link Input#jobAlert()}.
public record EmployeeHudState(
	EmployeeAlert alert,
	EmployeeStateIcons state,
	EmployeeEmotions emotion,
	float progress
) {

	/// Everything the HUD is computed from.
	///
	/// @param hasFoodPlace whether the employee has a food place assigned
	/// @param hasRestPlace whether the employee has a bed assigned
	/// @param jobAlert     the job's own alert (NO_WORKPLACE), or null when nothing is missing
	/// @param danger       surroundings threat, in [0, 1]
	/// @param fatigue      tiredness, in [0, 1]
	/// @param hunger       hunger, in [0, 1]
	/// @param work         work progress, in [0, 1]
	public record Input(
		boolean hasFoodPlace,
		boolean hasRestPlace,
		EmployeeAlert jobAlert,
		double danger,
		double fatigue,
		double hunger,
		double work
	) {}

	/// The thresholds the state icon is decided by.
	private static final double DANGER_THRESHOLD = 0.51;
	private static final double FATIGUE_THRESHOLD = 0.4;
	private static final double HUNGER_THRESHOLD = 0.6;

	/// Computes the HUD state: the scariest need wins, and the most urgent
	/// missing station wins.
	public static EmployeeHudState compute(Input in) {
		EmployeeStateIcons state = computeState(in);
		EmployeeAlert alert = computeAlert(in);
		float progress = (float) clamp(in.work(), 0.0, 1.0);
		return new EmployeeHudState(alert, state, EmployeeEmotions.HAPPY, progress);
	}

	private static EmployeeStateIcons computeState(Input in) {
		if (in.danger() >= DANGER_THRESHOLD) {
			return EmployeeStateIcons.SCARED;
		}
		if (in.fatigue() >= FATIGUE_THRESHOLD) {
			return EmployeeStateIcons.TIRED;
		}
		if (in.hunger() >= HUNGER_THRESHOLD) {
			return EmployeeStateIcons.HUNGRY;
		}
		return EmployeeStateIcons.GOOD;
	}

	private static EmployeeAlert computeAlert(Input in) {
		if (!in.hasFoodPlace()) {
			return EmployeeAlert.NO_FOOD_PLACE;
		}
		if (!in.hasRestPlace()) {
			return EmployeeAlert.NO_REST_PLACE;
		}
		if (in.jobAlert() != null) {
			return in.jobAlert();
		}
		return EmployeeAlert.GOOD;
	}

	private static double clamp(double value, double min, double max) {
		return Math.min(max, Math.max(min, value));
	}
}
