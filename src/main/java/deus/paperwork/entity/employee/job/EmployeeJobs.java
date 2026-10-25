package deus.paperwork.entity.employee.job;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/// The registry of employee jobs, keyed by their stable id.
///
/// The plain job is registered here, in the static initializer, so the
/// framework is autonomous: the base employee always has a job. Other jobs
/// (the farmer) register themselves from the mod initializer, which runs on
/// both the client and the server.
///
/// Looking a job up by an unknown id is an error, never a silent fallback to
/// the plain job: a mob with a broken job id must fail loudly instead of
/// silently behaving like a generic employee.
public final class EmployeeJobs {

	public static final String PLAIN_ID  = "plain";
	public static final String FARMER_ID = "farmer";
	public static final String GUARD_ID  = "guard";

	private static final Map<String, EmployeeJob> REGISTRY = new LinkedHashMap<>();

	static {
		register(new PlainJob());
	}

	private EmployeeJobs() {}

	/// Registers a job. Fails when its id is already taken.
	public static void register(EmployeeJob job) {
		String id = job.id();
		if (id == null || id.isBlank()) {
			throw new IllegalArgumentException("An employee job must have a non-blank id");
		}
		EmployeeJob existing = REGISTRY.putIfAbsent(id, job);
		if (existing != null) {
			throw new IllegalArgumentException("Employee job already registered: " + id);
		}
	}

	/// The job with that id.
	/// @throws IllegalArgumentException when no job is registered under that id.
	public static EmployeeJob byId(String id) {
		return find(id).orElseThrow(
			() -> new IllegalArgumentException("Unknown employee job id: " + id)
		);
	}

	/// The job with that id, or empty when there is none (no exception).
	public static Optional<EmployeeJob> find(String id) {
		if (id == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(REGISTRY.get(id));
	}

	/// Every registered job, in registration order.
	///
	/// It is only used by the tests, and it stays: a registry without an
	/// enumeration is lame, and the test of models.json uses it to protect
	/// the contract of the variants of EVERY registered job (the ones of
	/// today and the ones that come).
	public static Collection<EmployeeJob> all() {
		return List.copyOf(REGISTRY.values());
	}

	/// The plain job: the generic employee without a profession.
	public static EmployeeJob plain() {
		return byId(PLAIN_ID);
	}
}
