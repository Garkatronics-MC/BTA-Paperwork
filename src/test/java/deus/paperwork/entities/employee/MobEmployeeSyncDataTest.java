package deus.paperwork.entities.employee;

import com.mojang.nbt.tags.CompoundTag;
import deus.paperwork.entities.employee.enums.EmployeeAlert;
import deus.paperwork.entities.employee.enums.EmployeeEmotions;
import deus.paperwork.entities.employee.enums.EmployeeStateIcons;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The pure helpers of {@link MobEmployee} that translate the synced data.
///
/// The mob itself cannot be built in a test (it needs a BTA world with its
/// registry bootstrap), but the decoding of the HUD bytes and the comparison
/// of two stacks are pure and are the parts that can break silently.
class MobEmployeeSyncDataTest {

	@BeforeAll
	static void initItems() {
		Items.init();
	}

	@Test
	@DisplayName("every alert ordinal survives the round trip through the synced byte")
	void alertOrdinalsRoundTrip() {
		for (EmployeeAlert alert : EmployeeAlert.values()) {
			assertEquals(alert, MobEmployee.alertFromData((byte) alert.ordinal()));
		}
	}

	@Test
	@DisplayName("every state ordinal survives the round trip through the synced byte")
	void stateOrdinalsRoundTrip() {
		for (EmployeeStateIcons state : EmployeeStateIcons.values()) {
			assertEquals(state, MobEmployee.stateFromData((byte) state.ordinal()));
		}
	}

	@Test
	@DisplayName("every emotion ordinal survives the round trip through the synced byte")
	void emotionOrdinalsRoundTrip() {
		for (EmployeeEmotions emotion : EmployeeEmotions.values()) {
			assertEquals(emotion, MobEmployee.emotionFromData((byte) emotion.ordinal()));
		}
	}

	@Test
	@DisplayName("an alert out of range falls back to GOOD")
	void alertOutOfRangeIsGood() {
		assertEquals(EmployeeAlert.GOOD, MobEmployee.alertFromData((byte) 99));
		assertEquals(EmployeeAlert.GOOD, MobEmployee.alertFromData((byte) -1));
	}

	@Test
	@DisplayName("a state out of range falls back to GOOD")
	void stateOutOfRangeIsGood() {
		assertEquals(EmployeeStateIcons.GOOD, MobEmployee.stateFromData((byte) 99));
		assertEquals(EmployeeStateIcons.GOOD, MobEmployee.stateFromData((byte) -1));
	}

	@Test
	@DisplayName("an emotion out of range falls back to HAPPY")
	void emotionOutOfRangeIsHappy() {
		assertEquals(EmployeeEmotions.HAPPY, MobEmployee.emotionFromData((byte) 99));
		assertEquals(EmployeeEmotions.HAPPY, MobEmployee.emotionFromData((byte) -1));
	}

	@Test
	@DisplayName("two empty slots are the same")
	void emptySlotsAreTheSame() {
		assertTrue(MobEmployee.sameStack(null, null));
		assertTrue(MobEmployee.sameStack(null, new ItemStack(Items.WHEAT, 0)));
		assertTrue(MobEmployee.sameStack(new ItemStack(Items.WHEAT, 0), null));
	}

	@Test
	@DisplayName("two stacks of the same item and amount are the same, even when copied")
	void sameStacksAreTheSame() {
		ItemStack wheat = new ItemStack(Items.WHEAT, 5);
		ItemStack copy = wheat.copy();

		assertTrue(MobEmployee.sameStack(wheat, copy));
		assertTrue(MobEmployee.sameStack(wheat, wheat));
	}

	@Test
	@DisplayName("stacks with a different item, amount or data are not the same")
	void differentStacksAreNotTheSame() {
		assertFalse(MobEmployee.sameStack(new ItemStack(Items.WHEAT, 5), new ItemStack(Items.STICK, 5)));
		assertFalse(MobEmployee.sameStack(new ItemStack(Items.WHEAT, 5), new ItemStack(Items.WHEAT, 6)));
		assertFalse(MobEmployee.sameStack(new ItemStack(Items.WHEAT, 5), null));

		ItemStack named = new ItemStack(Items.WHEAT, 5);
		CompoundTag data = new CompoundTag();
		data.putString("Owner", "employee");
		named.setData(data);

		assertFalse(MobEmployee.sameStack(named, new ItemStack(Items.WHEAT, 5)));
	}
}
