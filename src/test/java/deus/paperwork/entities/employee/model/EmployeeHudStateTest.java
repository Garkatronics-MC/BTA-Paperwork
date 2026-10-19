package deus.paperwork.entities.employee.model;

import deus.paperwork.entities.employee.enums.EmployeeAlert;
import deus.paperwork.entities.employee.enums.EmployeeEmotions;
import deus.paperwork.entities.employee.enums.EmployeeStateIcons;
import deus.paperwork.entities.employee.model.EmployeeHudState.Conditions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmployeeHudStateTest {

	/// An employee with both stations, no job alert and no problem.
	private static Conditions healthy() {
		return new Conditions(true, true, null, 0.0, 0.0, 0.0, 1.0);
	}

	@Test
	void dangerMakesTheEmployeeScared() {
		Conditions in = new Conditions(true, true, null, 0.51, 0.0, 0.0, 1.0);
		assertEquals(EmployeeStateIcons.SCARED, EmployeeHudState.compute(in).state());
	}

	@Test
	void fatigueMakesTheEmployeeTired() {
		Conditions in = new Conditions(true, true, null, 0.0, 0.4, 0.0, 1.0);
		assertEquals(EmployeeStateIcons.TIRED, EmployeeHudState.compute(in).state());
	}

	@Test
	void hungerMakesTheEmployeeHungry() {
		Conditions in = new Conditions(true, true, null, 0.0, 0.0, 0.6, 1.0);
		assertEquals(EmployeeStateIcons.HUNGRY, EmployeeHudState.compute(in).state());
	}

	@Test
	void aFineEmployeeIsGood() {
		assertEquals(EmployeeStateIcons.GOOD, EmployeeHudState.compute(healthy()).state());
	}

	@Test
	void theScariestNeedWins() {
		// tired, hungry and scared: danger comes first
		Conditions in = new Conditions(true, true, null, 0.9, 0.9, 0.9, 1.0);
		assertEquals(EmployeeStateIcons.SCARED, EmployeeHudState.compute(in).state());

		// tired and hungry: tiredness comes first
		Conditions tired = new Conditions(true, true, null, 0.0, 0.9, 0.9, 1.0);
		assertEquals(EmployeeStateIcons.TIRED, EmployeeHudState.compute(tired).state());
	}

	@Test
	void aMissingFoodPlaceIsTheFirstAlert() {
		Conditions in = new Conditions(false, false, EmployeeAlert.NO_WORKPLACE, 0.0, 0.0, 0.0, 1.0);
		assertEquals(EmployeeAlert.NO_FOOD_PLACE, EmployeeHudState.compute(in).alert());
	}

	@Test
	void aMissingRestPlaceComesBeforeTheJobAlert() {
		Conditions in = new Conditions(true, false, EmployeeAlert.NO_WORKPLACE, 0.0, 0.0, 0.0, 1.0);
		assertEquals(EmployeeAlert.NO_REST_PLACE, EmployeeHudState.compute(in).alert());
	}

	@Test
	void theJobAlertShowsWhenNothingElseIsMissing() {
		Conditions in = new Conditions(true, true, EmployeeAlert.NO_WORKPLACE, 0.0, 0.0, 0.0, 1.0);
		assertEquals(EmployeeAlert.NO_WORKPLACE, EmployeeHudState.compute(in).alert());
	}

	@Test
	void withoutAnyProblemTheAlertIsGood() {
		assertEquals(EmployeeAlert.GOOD, EmployeeHudState.compute(healthy()).alert());
	}

	@Test
	void theProgressIsClamped() {
		Conditions tooMuch = new Conditions(true, true, null, 0.0, 0.0, 0.0, 4.2);
		assertEquals(1.0f, EmployeeHudState.compute(tooMuch).progress());

		Conditions tooLittle = new Conditions(true, true, null, 0.0, 0.0, 0.0, -1.0);
		assertEquals(0.0f, EmployeeHudState.compute(tooLittle).progress());

		Conditions justRight = new Conditions(true, true, null, 0.0, 0.0, 0.0, 0.25);
		assertEquals(0.25f, EmployeeHudState.compute(justRight).progress());

		// the same clipping when the progress comes from the synced data
		assertEquals(1.0f, decodeProgress((short) 4000));
		assertEquals(0.0f, decodeProgress((short) -100));
		assertEquals(0.25f, decodeProgress((short) 250));
	}

	@Test
	void theEmotionIsAlwaysHappy() {
		Conditions in = new Conditions(false, false, EmployeeAlert.NO_WORKPLACE, 1.0, 1.0, 1.0, 0.0);
		assertEquals(EmployeeEmotions.HAPPY, EmployeeHudState.compute(in).emotion());
		assertEquals(EmployeeEmotions.HAPPY, EmployeeHudState.compute(healthy()).emotion());

		// and the decoding of the synced bytes falls back to it as well
		EmployeeHudState decoded = EmployeeHudState.fromOrdinals(
			(byte) EmployeeAlert.GOOD.ordinal(),
			(byte) EmployeeStateIcons.GOOD.ordinal(),
			(byte) 99,
			(short) 0
		);
		assertEquals(EmployeeEmotions.HAPPY, decoded.emotion());
	}

	/// The progress the client decodes out of the synced short.
	private static float decodeProgress(short progress) {
		return EmployeeHudState.fromOrdinals(
			(byte) EmployeeAlert.GOOD.ordinal(),
			(byte) EmployeeStateIcons.GOOD.ordinal(),
			(byte) EmployeeEmotions.HAPPY.ordinal(),
			progress
		).progress();
	}
}
