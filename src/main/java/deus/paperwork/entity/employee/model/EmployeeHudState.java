package deus.paperwork.entity.employee.model;

import deus.paperwork.entity.employee.enums.EmployeeAlert;
import deus.paperwork.entity.employee.enums.EmployeeEmotions;
import deus.paperwork.entity.employee.enums.EmployeeStateIcons;

/// The HUD state of an employee: the alert icon, the state icon, the emotion
/// and the work progress bar drawn over its head.
///
/// It is pure: the server computes it and publishes it, the client only
/// draws it. The job's own alert (the farmer's missing workplace) enters
/// through {@link Conditions#jobAlert()}.
public record EmployeeHudState(
	EmployeeAlert alert,
	EmployeeStateIcons state,
	EmployeeEmotions emotion,
	float progress
) {

	/// The scale of the work progress on the wire: it travels as a short
	/// (SynchedEntityData has no float getter), so 1000 means 1.0.
	public static final float PROGRESS_SCALE = 1000f;

	/// Everything the HUD is computed from: the stations the employee has, the
	/// alert of its job and its stats.
	///
	/// @param hasFoodPlace whether the employee has a food place assigned
	/// @param hasRestPlace whether the employee has a bed assigned
	/// @param jobAlert     the job's own alert (NO_WORKPLACE), or null when nothing is missing
	/// @param danger       surroundings threat, in [0, 1]
	/// @param fatigue      tiredness, in [0, 1]
	/// @param hunger       hunger, in [0, 1]
	/// @param work         work progress, in [0, 1]
	public record Conditions(
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
	///
	/// The priority of the alert is the one the HUD had before the refactor:
	/// NO_FOOD_PLACE, then NO_REST_PLACE, then the alert of the job, and
	/// GOOD when nothing is missing. It is kept as it is so the refactor does
	/// not degrade the information the HUD already gave. The alert of the job
	/// shows up for the employee that is set up (it has food and a bed) but
	/// misses its own station: exactly the case the spec wants to see (the
	/// farmer without a workplace).
	public static EmployeeHudState compute(Conditions in) {
		EmployeeStateIcons state = computeState(in);
		EmployeeAlert alert = computeAlert(in);
		float progress = (float) clamp(in.work(), 0.0, 1.0);
		return new EmployeeHudState(alert, state, EmployeeEmotions.HAPPY, progress);
	}

	/// Decodes the HUD state published by the server through the synced data.
	///
	/// The three icons travel as the ordinal of their enum and the progress
	/// as a short scaled by {@link #PROGRESS_SCALE}. The decoding is
	/// defensive: an ordinal out of range falls back to the neutral one
	/// (GOOD / GOOD / HAPPY) and the progress is clipped to [0, 1].
	public static EmployeeHudState fromOrdinals(byte alert, byte state, byte emotion, short progress) {
		return new EmployeeHudState(
			fromOrdinal(EmployeeAlert.values(), alert, EmployeeAlert.GOOD),
			fromOrdinal(EmployeeStateIcons.values(), state, EmployeeStateIcons.GOOD),
			fromOrdinal(EmployeeEmotions.values(), emotion, EmployeeEmotions.HAPPY),
			(float) clamp(progress / PROGRESS_SCALE, 0.0, 1.0)
		);
	}

	/// The enum value of an ordinal, or the fallback when it is out of range.
	private static <E extends Enum<E>> E fromOrdinal(E[] values, byte raw, E fallback) {
		int ordinal = raw & 0xFF;
		return ordinal < values.length ? values[ordinal] : fallback;
	}

	private static EmployeeStateIcons computeState(Conditions in) {
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

	private static EmployeeAlert computeAlert(Conditions in) {
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
