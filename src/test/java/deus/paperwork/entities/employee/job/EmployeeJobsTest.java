package deus.paperwork.entities.employee.job;

import deus.brainless.agent.Agent;
import deus.paperwork.entities.employee.MobEmployee;
import deus.paperwork.entities.employee.enums.EmployeeAlert;
import deus.paperwork.entities.employee.model.EmployeeStats;
import deus.paperwork.entities.employee.model.EmployeeStations;
import deus.paperwork.entities.employee.model.EmployeeTraits;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmployeeJobsTest {

	@Test
	void byIdReturnsThePlainJob() {
		EmployeeJob job = EmployeeJobs.byId(EmployeeJobs.PLAIN_ID);
		assertEquals("plain", job.id());
		assertInstanceOf(PlainJob.class, job);
	}

	@Test
	void plainIsThePlainJob() {
		assertSame(EmployeeJobs.byId(EmployeeJobs.PLAIN_ID), EmployeeJobs.plain());
	}

	@Test
	void byIdThrowsOnUnknownId() {
		IllegalArgumentException error = assertThrows(
			IllegalArgumentException.class,
			() -> EmployeeJobs.byId("nope")
		);
		// the message must name the offending id, not fall back to plain
		assertTrue(error.getMessage().contains("nope"), error.getMessage());
	}

	@Test
	void byIdNeverFallsBackToPlain() {
		// an id no job will ever take: the lookup must fail, not return plain
		assertThrows(IllegalArgumentException.class, () -> EmployeeJobs.byId("no_such_job"));
	}

	@Test
	void findReturnsEmptyOnUnknownId() {
		assertTrue(EmployeeJobs.find("nope").isEmpty());
	}

	@Test
	void findReturnsTheJobOnKnownId() {
		assertTrue(EmployeeJobs.find(EmployeeJobs.PLAIN_ID).isPresent());
		assertEquals("plain", EmployeeJobs.find("plain").orElseThrow().id());
	}

	@Test
	void findToleratesNullId() {
		assertTrue(EmployeeJobs.find(null).isEmpty());
	}

	@Test
	void registerFailsOnDuplicateId() {
		assertThrows(
			IllegalArgumentException.class,
			() -> EmployeeJobs.register(new StubJob(EmployeeJobs.PLAIN_ID))
		);
	}

	@Test
	void failedRegistrationLeavesTheRegistryUntouched() {
		assertThrows(
			IllegalArgumentException.class,
			() -> EmployeeJobs.register(new StubJob(EmployeeJobs.PLAIN_ID))
		);
		EmployeeJob job = EmployeeJobs.byId(EmployeeJobs.PLAIN_ID);
		assertInstanceOf(PlainJob.class, job);
	}

	@Test
	void allContainsThePlainJob() {
		assertTrue(EmployeeJobs.all().stream().anyMatch(j -> j.id().equals(EmployeeJobs.PLAIN_ID)));
	}

	/// A job that only knows its id: enough to exercise the registry.
	private static final class StubJob implements EmployeeJob {

		private final String id;

		private StubJob(String id) {
			this.id = id;
		}

		@Override
		public String id() {
			return id;
		}

		@Override
		public List<EmployeeAccessory> accessories() {
			return List.of();
		}

		@Override
		public EmployeeModelSpec modelSpec(MobEmployee mob) {
			return EmployeeModelSpec.empty();
		}

		@Override
		public void work(MobEmployee mob) {}

		@Override
		public Agent buildAgent(MobEmployee mob, EmployeeTraits traits, EmployeeStats stats, EmployeeStations stations) {
			throw new UnsupportedOperationException("not needed for the registry");
		}

		@Override
		public EmployeeStations stations(MobEmployee mob, EmployeeStations base) {
			return base;
		}

		@Override
		public void recharge(MobEmployee mob, EmployeeStats stats) {}

		@Override
		public Optional<EmployeeAlert> missingStationAlert(MobEmployee mob) {
			return Optional.empty();
		}

		@Override
		public void clearStations(MobEmployee mob) {}

		@Override
		public EmployeeJobState createState() {
			return new EmployeeJobState();
		}
	}
}
