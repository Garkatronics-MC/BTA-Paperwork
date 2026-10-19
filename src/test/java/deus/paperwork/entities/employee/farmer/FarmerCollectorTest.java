package deus.paperwork.entities.employee.farmer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The decision of the farmer to look for seeds in a chest is pure, so it can
/// be checked without a mob.
class FarmerCollectorTest {

	@Test
	@DisplayName("without seeds the farmer looks for them")
	void withoutSeedsItLooksForThem() {
		assertTrue(FarmerCollector.wantsSeeds(false, 0));
		assertTrue(FarmerCollector.wantsSeeds(false, 20));
		assertTrue(FarmerCollector.wantsSeeds(false, 100));
	}

	@Test
	@DisplayName("with seeds the farmer does not look for more")
	void withSeedsItDoesNotLookForMore() {
		assertFalse(FarmerCollector.wantsSeeds(true, 0));
		assertFalse(FarmerCollector.wantsSeeds(true, 20));
	}

	@Test
	@DisplayName("it does not look for seeds on every tick")
	void itDoesNotLookOnEveryTick() {
		assertFalse(FarmerCollector.wantsSeeds(false, 1));
		assertFalse(FarmerCollector.wantsSeeds(false, 19));
		assertFalse(FarmerCollector.wantsSeeds(false, 21));
	}
}
