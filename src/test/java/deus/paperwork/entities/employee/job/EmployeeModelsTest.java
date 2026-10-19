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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The models.json of the employees must know the body of the employee and the
/// accessory of every job, otherwise the renderer falls back to another model
/// (or to the fallback model) at runtime, silently.
///
/// The test reads the real file from the classpath, so a rename of a geometry
/// or a missing accessory fails here, not in the game.
class EmployeeModelsTest {

	private static final String MODELS_PATH = "/assets/paperwork/models/entity/models.json";

	/// The entry of models.json the employee renderer resolves its models in.
	private static final String EMPLOYEE_ENTRY = "paperwork:employee";

	/// The entry of the deprecated farmer entity, which stays for the old
	/// saves.
	private static final String FARMER_ENTRY = "paperwork:employee_farmer";

	/// The model of the body of every employee.
	private static final String BODY_MODEL = "main";

	/// The model of the bag of the farmer: the accessory of its job.
	private static final String FARMER_BAG_MODEL = "farmer_bag";

	@Test
	void everyEmployeeHasTheBodyModel() {
		JsonObject entry = employeeEntry();

		assertTrue(entry.has(BODY_MODEL), "models.json has no '" + BODY_MODEL + "' model");
		assertEquals("geometry.paperwork.employee", entry.get(BODY_MODEL).getAsString());
	}

	@Test
	void everyRegisteredJobHasItsAccessories() {
		JsonObject entry = employeeEntry();

		for (EmployeeJob job : EmployeeJobs.all()) {
			for (EmployeeAccessory accessory : job.accessories()) {
				assertTrue(entry.has(accessory.model()),
					"models.json has no '" + accessory.model() + "' model for the job " + job.id());
				assertTrue(entry.get(accessory.model()).getAsString().startsWith("geometry.paperwork."),
					"The accessory '" + accessory.model() + "' of the job " + job.id()
						+ " points to " + entry.get(accessory.model()).getAsString());
			}
		}
	}

	/// The farmer job is registered by the mod initializer, which does not run
	/// in the unit tests, so its accessory is checked as a plain string.
	@Test
	void theBagOfTheFarmerIsDeclared() {
		JsonObject entry = employeeEntry();

		assertTrue(entry.has(FARMER_BAG_MODEL), "models.json has no '" + FARMER_BAG_MODEL + "' model");
		assertEquals("geometry.paperwork.accessories.farmer_bag", entry.get(FARMER_BAG_MODEL).getAsString());
	}

	/// The deprecated farmer entity keeps its own entry: its renderer resolves
	/// the models of it.
	@Test
	void theDeprecatedFarmerEntryIsStillThere() {
		JsonObject renderers = renderers();
		assertTrue(renderers.has(FARMER_ENTRY), "models.json has no '" + FARMER_ENTRY + "' entry");

		JsonObject entry = renderers.get(FARMER_ENTRY).getAsJsonObject();
		assertTrue(entry.has(BODY_MODEL), "'" + FARMER_ENTRY + "' has no '" + BODY_MODEL + "' model");
		assertTrue(entry.has(FARMER_BAG_MODEL), "'" + FARMER_ENTRY + "' has no '" + FARMER_BAG_MODEL + "' model");

		assertEquals("geometry.paperwork.employee", entry.get(BODY_MODEL).getAsString());
		assertEquals("geometry.paperwork.accessories.farmer_bag", entry.get(FARMER_BAG_MODEL).getAsString());
	}

	/// Every geometry the employees use must be loaded: a geometry missing
	/// from the model paths would not be found at runtime.
	@Test
	void everyEmployeeGeometryIsLoaded() {
		JsonObject entry = employeeEntry();
		List<String> loaded = loadedGeometries();

		for (String key : entry.keySet()) {
			String geometry = entry.get(key).getAsString();
			assertTrue(loaded.contains(geometry), "The geometry " + geometry + " is not in the model paths");
		}
	}

	/// The geometries that the loaded geo files declare.
	private static List<String> loadedGeometries() {
		List<String> geometries = new ArrayList<>();

		for (String path : modelPaths()) {
			try (InputStream stream = EmployeeModelsTest.class.getResourceAsStream(path)) {
				assertNotNull(stream, path + " is not on the classpath");
				try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
					JsonObject geo = JsonParser.parseReader(reader).getAsJsonObject();
					for (JsonElement element : geo.getAsJsonArray("minecraft:geometry")) {
						geometries.add(element.getAsJsonObject()
							.getAsJsonObject("description")
							.get("identifier")
							.getAsString());
					}
				}
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		return geometries;
	}

	@Test
	void theModelPathsAreNotTouched() {
		List<String> paths = modelPaths();

		assertTrue(paths.contains("/assets/paperwork/models/entity/employee/employee.geo.json"));
		assertTrue(paths.contains("/assets/paperwork/models/entity/employee/accessories/farmer_bag.geo.json"));
	}

	// ------------------------------------------------------------------ helpers

	/// The whole file, as it is on the classpath.
	private static JsonObject models() {
		try (InputStream stream = EmployeeModelsTest.class.getResourceAsStream(MODELS_PATH)) {
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

	/// The entry the employee renderer resolves its models in.
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
}
