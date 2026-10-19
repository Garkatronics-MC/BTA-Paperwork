package deus.paperwork.entities.employee.model;

import deus.paperwork.entities.employee.enums.EmployeeAlert;
import deus.paperwork.entities.employee.enums.EmployeeEmotions;
import deus.paperwork.entities.employee.enums.EmployeeStateIcons;
import deus.paperwork.entities.employee.model.EmployeeHudState.Input;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmployeeHudStateTest {

	/// An employee with both stations, no job alert and no problem.
	private static Input healthy() {
		return new Input(true, true, null, 0.0, 0.0, 0.0, 1.0);
	}

	@Test
	void dangerMakesTheEmployeeScared() {
		Input in = new Input(true, true, null, 0.51, 0.0, 0.0, 1.0);
		assertEquals(EmployeeStateIcons.SCARED, EmployeeHudState.compute(in).state());
	}

	@Test
	void fatigueMakesTheEmployeeTired() {
		Input in = new Input(true, true, null, 0.0, 0.4, 0.0, 1.0);
		assertEquals(EmployeeStateIcons.TIRED, EmployeeHudState.compute(in).state());
	}

	@Test
	void hungerMakesTheEmployeeHungry() {
		Input in = new Input(true, true, null, 0.0, 0.0, 0.6, 1.0);
		assertEquals(EmployeeStateIcons.HUNGRY, EmployeeHudState.compute(in).state());
	}

	@Test
	void aFineEmployeeIsGood() {
		assertEquals(EmployeeStateIcons.GOOD, EmployeeHudState.compute(healthy()).state());
	}

	@Test
	void theScariestNeedWins() {
		// tired, hungry and scared: danger comes first
		Input in = new Input(true, true, null, 0.9, 0.9, 0.9, 1.0);
		assertEquals(EmployeeStateIcons.SCARED, EmployeeHudState.compute(in).state());

		// tired and hungry: tiredness comes first
		Input tired = new Input(true, true, null, 0.0, 0.9, 0.9, 1.0);
		assertEquals(EmployeeStateIcons.TIRED, EmployeeHudState.compute(tired).state());
	}

	@Test
	void aMissingFoodPlaceIsTheFirstAlert() {
		Input in = new Input(false, false, EmployeeAlert.NO_WORKPLACE, 0.0, 0.0, 0.0, 1.0);
		assertEquals(EmployeeAlert.NO_FOOD_PLACE, EmployeeHudState.compute(in).alert());
	}

	@Test
	void aMissingRestPlaceComesBeforeTheJobAlert() {
		Input in = new Input(true, false, EmployeeAlert.NO_WORKPLACE, 0.0, 0.0, 0.0, 1.0);
		assertEquals(EmployeeAlert.NO_REST_PLACE, EmployeeHudState.compute(in).alert());
	}

	@Test
	void theJobAlertShowsWhenNothingElseIsMissing() {
		Input in = new Input(true, true, EmployeeAlert.NO_WORKPLACE, 0.0, 0.0, 0.0, 1.0);
		assertEquals(EmployeeAlert.NO_WORKPLACE, EmployeeHudState.compute(in).alert());
	}

	@Test
	void withoutAnyProblemTheAlertIsGood() {
		assertEquals(EmployeeAlert.GOOD, EmployeeHudState.compute(healthy()).alert());
	}

	@Test
	void theProgressIsClamped() {
		Input tooMuch = new Input(true, true, null, 0.0, 0.0, 0.0, 4.2);
		assertEquals(1.0f, EmployeeHudState.compute(tooMuch).progress());

		Input tooLittle = new Input(true, true, null, 0.0, 0.0, 0.0, -1.0);
		assertEquals(0.0f, EmployeeHudState.compute(tooLittle).progress());

		Input justRight = new Input(true, true, null, 0.0, 0.0, 0.0, 0.25);
		assertEquals(0.25f, EmployeeHudState.compute(justRight).progress());
	}

	@Test
	void theEmotionIsAlwaysHappy() {
		Input in = new Input(false, false, EmployeeAlert.NO_WORKPLACE, 1.0, 1.0, 1.0, 0.0);
		assertEquals(EmployeeEmotions.HAPPY, EmployeeHudState.compute(in).emotion());
		assertEquals(EmployeeEmotions.HAPPY, EmployeeHudState.compute(healthy()).emotion());
	}
}
