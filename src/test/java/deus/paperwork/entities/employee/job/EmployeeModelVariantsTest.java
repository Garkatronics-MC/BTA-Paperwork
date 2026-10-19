package deus.paperwork.entities.employee.job;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The models.json of the employees must know every model variant of every
/// job, otherwise the renderer falls back to another model (or to the
/// fallback model) at runtime, silently.
///
/// The test reads the real file from the classpath, so a rename of a
/// geometry or a missing variant fails here, not in the game.
class EmployeeModelVariantsTest {

	private static final String MODELS_PATH = "/assets/paperwork/models/entity/models.json";

	/// The entry of models.json the employee renderer resolves its variants in.
	private static final String EMPLOYEE_ENTRY = "paperwork:employee";

	/// The entry of the deprecated farmer entity, which stays for the old
	/// saves.
	private static final String FARMER_ENTRY = "paperwork:employee_farmer";

	/// Every geometry of an employee starts with this prefix.
	private static final String EMPLOYEE_GEOMETRY_PREFIX = "geometry.paperwork.employee";

	@Test
	void everyRegisteredJobHasBothVariants() {
		JsonObject entry = employeeEntry();

		for (EmployeeJob job : EmployeeJobs.all()) {
			String manKey = job.modelVariant(false);
			String womanKey = job.modelVariant(true);

			assertTrue(entry.has(manKey), "models.json has no '" + manKey + "' variant for the job " + job.id());
			assertTrue(entry.has(womanKey), "models.json has no '" + womanKey + "' variant for the job " + job.id());

			assertTrue(entry.get(manKey).getAsString().startsWith(EMPLOYEE_GEOMETRY_PREFIX),
				"The variant '" + manKey + "' of the job " + job.id() + " points to " + entry.get(manKey).getAsString());
			assertTrue(entry.get(womanKey).getAsString().startsWith(EMPLOYEE_GEOMETRY_PREFIX),
				"The variant '" + womanKey + "' of the job " + job.id() + " points to " + entry.get(womanKey).getAsString());
		}
	}

	@Test
	void theTwoGendersOfAJobUseDifferentGeometries() {
		JsonObject entry = employeeEntry();

		for (EmployeeJob job : EmployeeJobs.all()) {
			String man = entry.get(job.modelVariant(false)).getAsString();
			String woman = entry.get(job.modelVariant(true)).getAsString();
			assertNotEquals(man, woman, "The job " + job.id() + " uses the same geometry for both genders");
		}
	}

	@Test
	void thePlainJobPointsToTheEmployeeGeometries() {
		JsonObject entry = employeeEntry();

		assertEquals("geometry.paperwork.employee", entry.get("man").getAsString());
		assertEquals("geometry.paperwork.employee_woman", entry.get("woman").getAsString());
	}

	/// The farmer job is registered by the mod initializer, which does not run
	/// in the unit tests, so its variants are checked as plain strings.
	@Test
	void theFarmerVariantsAreDeclared() {
		JsonObject entry = employeeEntry();

		assertTrue(entry.has("farmer_man"), "models.json has no 'farmer_man' variant");
		assertTrue(entry.has("farmer_woman"), "models.json has no 'farmer_woman' variant");

		assertEquals("geometry.paperwork.employee_farmer", entry.get("farmer_man").getAsString());
		assertEquals("geometry.paperwork.employee_woman_farmer", entry.get("farmer_woman").getAsString());
	}

	/// The deprecated farmer entity keeps its own entry: its renderer resolves
	/// the 'man' and 'woman' variants of it.
	@Test
	void theDeprecatedFarmerEntryIsStillThere() {
		JsonObject renderers = renderers();
		assertTrue(renderers.has(FARMER_ENTRY), "models.json has no '" + FARMER_ENTRY + "' entry");

		JsonObject entry = renderers.get(FARMER_ENTRY).getAsJsonObject();
		assertTrue(entry.has("man"), "'" + FARMER_ENTRY + "' has no 'man' variant");
		assertTrue(entry.has("woman"), "'" + FARMER_ENTRY + "' has no 'woman' variant");

		assertEquals("geometry.paperwork.employee_farmer", entry.get("man").getAsString());
		assertEquals("geometry.paperwork.employee_woman_farmer", entry.get("woman").getAsString());
	}

	/// Every geometry the employees use must be loaded: a geometry missing
	/// from the model paths would not be found at runtime.
	@Test
	void everyEmployeeGeometryIsLoaded() {
		JsonObject entry = employeeEntry();
		List<String> paths = modelPaths();

		for (String key : entry.keySet()) {
			String geometry = entry.get(key).getAsString();
			assertTrue(paths.contains(geoPathOf(geometry)), "The geometry " + geometry + " is not in the model paths");
		}
	}

	@Test
	void theModelPathsAreNotTouched() {
		List<String> paths = modelPaths();

		assertTrue(paths.contains("/assets/paperwork/models/entity/employee.geo.json"));
		assertTrue(paths.contains("/assets/paperwork/models/entity/employee_woman.geo.json"));
		assertTrue(paths.contains("/assets/paperwork/models/entity/employee_farmer.geo.json"));
		assertTrue(paths.contains("/assets/paperwork/models/entity/employee_woman_farmer.geo.json"));
	}

	// ------------------------------------------------------------------ helpers

	/// The whole file, as it is on the classpath.
	private static JsonObject models() {
		try (InputStream stream = EmployeeModelVariantsTest.class.getResourceAsStream(MODELS_PATH)) {
			assertNotNull(stream, MODELS_PATH + " is not on the classpath");
			try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
				return JsonParser.parseReader(reader).getAsJsonObject();
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/// The renderers of models.json.
	private static JsonObject renderers() {
		JsonObject models = models();
		assertTrue(models.has("renderers"), "models.json has no 'renderers' section");
		return models.get("renderers").getAsJsonObject();
	}

	/// The entry the employee renderer resolves its variants in.
	private static JsonObject employeeEntry() {
		JsonObject renderers = renderers();
		assertTrue(renderers.has(EMPLOYEE_ENTRY), "models.json has no '" + EMPLOYEE_ENTRY + "' entry");
		return renderers.get(EMPLOYEE_ENTRY).getAsJsonObject();
	}

	/// The geo files that are loaded.
	private static List<String> modelPaths() {
		JsonObject models = models();
		assertTrue(models.has("model_paths"), "models.json has no 'model_paths' section");

		List<String> paths = new ArrayList<>();
		for (JsonElement element : models.getAsJsonArray("model_paths")) {
			paths.add(element.getAsString());
		}
		return paths;
	}

	/// The file of a geometry: "geometry.paperwork.employee" lives in
	/// "/assets/paperwork/models/entity/employee.geo.json".
	private static String geoPathOf(String geometry) {
		assertTrue(geometry.startsWith("geometry.paperwork."), "Unexpected geometry: " + geometry);
		String name = geometry.substring("geometry.paperwork.".length());
		return "/assets/paperwork/models/entity/" + name + ".geo.json";
	}
}
