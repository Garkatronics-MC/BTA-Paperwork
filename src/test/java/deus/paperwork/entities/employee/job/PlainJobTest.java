package deus.paperwork.entities.employee.job;

import deus.paperwork.entities.employee.model.EmployeeStations;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlainJobTest {

	/// The plain job ignores the mob, so the tests stay pure: no World, no
	/// entity instantiation.
	private static final PlainJob JOB = new PlainJob();

	@Test
	void idIsPlain() {
		assertEquals("plain", JOB.id());
		assertEquals(EmployeeJobs.PLAIN_ID, JOB.id());
	}

	@Test
	void modelVariantDependsOnTheGender() {
		assertEquals("man", JOB.modelVariant(false));
		assertEquals("woman", JOB.modelVariant(true));
	}

	@Test
	void textureFolderIsEmployee() {
		assertEquals("employee", JOB.textureFolder());
	}

	@Test
	void stationsAreTheBaseOnes() {
		EmployeeStations base = EmployeeStations.none();

		EmployeeStations result = JOB.stations(null, base);

		assertSame(base, result);
		assertTrue(result.workPlace().get().isEmpty());
	}

	@Test
	void modelSpecIsEmpty() {
		assertTrue(JOB.modelSpec(null).bones().isEmpty());
	}

	@Test
	void thereIsNoMissingStationAlert() {
		assertTrue(JOB.missingStationAlert(null).isEmpty());
	}

	@Test
	void clearingStationsDoesNothing() {
		JOB.clearStations(null);
	}

	@Test
	void theStateIsFreshAndEmpty() {
		EmployeeJobState state = JOB.createState();
		assertNotNull(state);
		assertEquals(EmployeeJobState.class, state.getClass());
	}
}
