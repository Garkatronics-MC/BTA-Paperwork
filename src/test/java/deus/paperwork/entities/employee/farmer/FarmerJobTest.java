package deus.paperwork.entities.employee.farmer;

import deus.paperwork.entities.employee.job.EmployeeAccessory;
import deus.paperwork.entities.employee.job.EmployeeJobs;
import deus.paperwork.entities.employee.job.EmployeeModelSpec.EmployeeBone;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The farmer job without a mob: the ids, the variants and the wheat bones.
///
/// The job is a singleton without state, so everything that does not need a
/// World can be checked here.
class FarmerJobTest {

	/// The job is used directly (not through the registry): the registry is
	/// filled by the mod initializer, which does not run in the tests.
	private static final FarmerJob JOB = new FarmerJob();

	@Test
	@DisplayName("the job is the farmer")
	void idIsFarmer() {
		assertEquals("farmer", JOB.id());
		assertEquals(EmployeeJobs.FARMER_ID, JOB.id());
	}

	@Test
	@DisplayName("the farmer carries its bag")
	void theFarmerCarriesItsBag() {
		List<EmployeeAccessory> accessories = JOB.accessories();

		assertEquals(1, accessories.size());
		assertEquals("farmer_bag", accessories.get(0).model());
		assertTrue(accessories.get(0).texture().endsWith("farmer_bag.png"));
	}

	@Test
	@DisplayName("the state of a farmer is a farmer state")
	void theStateIsAFarmerState() {
		assertNotNull(JOB.createState());
		assertEquals(FarmerJobState.class, JOB.createState().getClass());
	}

	@Test
	@DisplayName("without wheat the three wheat bones are hidden")
	void withoutWheatEveryBoneIsHidden() {
		List<EmployeeBone> bones = FarmerJob.wheatBones(0);

		assertEquals(3, bones.size());
		for (EmployeeBone bone : bones) {
			assertFalse(bone.visible(), bone.name() + " must be hidden");
		}
	}

	@Test
	@DisplayName("with one wheat only the first bone shows up")
	void withOneWheatOnlyTheFirstBoneShowsUp() {
		assertBone(FarmerJob.wheatBones(1), "wheat1", true);
		assertBone(FarmerJob.wheatBones(1), "wheat2", false);
		assertBone(FarmerJob.wheatBones(1), "wheat3", false);
	}

	@Test
	@DisplayName("with 32 wheat the second bone shows up too")
	void withThirtyTwoWheatTwoBonesShowUp() {
		assertBone(FarmerJob.wheatBones(32), "wheat1", true);
		assertBone(FarmerJob.wheatBones(32), "wheat2", true);
		assertBone(FarmerJob.wheatBones(32), "wheat3", false);
	}

	@Test
	@DisplayName("with 64 wheat the three bones show up")
	void withSixtyFourWheatEveryBoneShowsUp() {
		assertBone(FarmerJob.wheatBones(64), "wheat1", true);
		assertBone(FarmerJob.wheatBones(64), "wheat2", true);
		assertBone(FarmerJob.wheatBones(64), "wheat3", true);
	}

	@Test
	@DisplayName("the bones are always the three of them, hidden or not")
	void theThreeBonesAreAlwaysThere() {
		for (int wheat : new int[] {0, 1, 31, 32, 63, 64, 200}) {
			List<EmployeeBone> bones = FarmerJob.wheatBones(wheat);
			assertEquals(3, bones.size(), "wheat " + wheat + " must give the three wheat bones");
			assertEquals("wheat1", bones.get(0).name());
			assertEquals("wheat2", bones.get(1).name());
			assertEquals("wheat3", bones.get(2).name());
		}
	}

	private static void assertBone(List<EmployeeBone> bones, String name, boolean visible) {
		EmployeeBone bone = bones.stream()
			.filter(b -> b.name().equals(name))
			.findFirst()
			.orElseThrow(() -> new AssertionError("the bone " + name + " is missing"));
		assertEquals(visible, bone.visible(), "the bone " + name + " visibility");
		assertTrue(bone.pitch() == null && bone.yaw() == null && bone.roll() == null,
			"the wheat bones are only shown or hidden, never posed");
	}
}
