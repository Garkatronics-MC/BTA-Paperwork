package deus.paperwork.entity.employee.guard;

import deus.paperwork.entity.employee.job.EmployeeJobState;
import deus.paperwork.entity.employee.job.EmployeeJobs;
import deus.paperwork.entity.employee.model.EmployeeStations;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The guard job without a mob: the id, the look and the state.
///
/// The fight itself needs a World and living entities, so it is covered by the
/// game checks; here we protect the parts that must not drift.
class GuardJobTest {

	/// The job is used directly (not through the registry): the registry is
	/// filled by the mod initializer, which does not run in the tests.
	private static final GuardJob JOB = new GuardJob();

	@Test
	@DisplayName("the job is the guard")
	void idIsGuard() {
		assertEquals("guard", JOB.id());
		assertEquals(EmployeeJobs.GUARD_ID, JOB.id());
	}

	@Test
	@DisplayName("the guard has no accessory yet")
	void theGuardHasNoAccessory() {
		assertTrue(JOB.accessories().isEmpty());
	}

	@Test
	@DisplayName("the guard skeleton has no extra bones")
	void modelSpecIsEmpty() {
		assertTrue(JOB.modelSpec(null).bones().isEmpty());
	}

	@Test
	@DisplayName("the guard uses the base stations")
	void stationsAreTheBaseOnes() {
		EmployeeStations base = EmployeeStations.none();

		EmployeeStations result = JOB.stations(null, base);

		assertSame(base, result);
		assertTrue(result.workPlace().get().isEmpty());
	}

	@Test
	@DisplayName("a guard without a workplace is not complaining")
	void thereIsNoMissingStationAlert() {
		assertTrue(JOB.missingStationAlert(null).isEmpty());
	}

	@Test
	@DisplayName("the guard needs nothing specific in its work area")
	void thereIsNoMissingZoneItem() {
		assertNull(JOB.missingZoneItems(null, null));
	}

	@Test
	@DisplayName("a guard without a world has no work to do")
	void withoutMobThereIsNoWork() {
		assertFalse(JOB.hasWorkToDo(null));
	}

	@Test
	@DisplayName("clearing the stations of a guard does not break")
	void clearingStationsDoesNothing() {
		JOB.clearStations(null);
		JOB.recharge(null, null);
		JOB.work(null);
	}

	@Test
	@DisplayName("the state of a guard is a fresh employee state")
	void theStateIsFreshAndEmpty() {
		EmployeeJobState state = JOB.createState();

		assertNotNull(state);
		assertEquals(EmployeeJobState.class, state.getClass());
	}

	@Test
	@DisplayName("a null target is never a hostile")
	void aNullTargetIsNotHostile() {
		assertFalse(GuardTargeting.isHostile(null));
	}
}
