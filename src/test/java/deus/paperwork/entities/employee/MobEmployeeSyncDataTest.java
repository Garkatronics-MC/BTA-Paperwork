package deus.paperwork.entities.employee;

import com.mojang.nbt.tags.CompoundTag;
import deus.paperwork.entities.employee.enums.EmployeeAlert;
import deus.paperwork.entities.employee.enums.EmployeeEmotions;
import deus.paperwork.entities.employee.enums.EmployeeStateIcons;
import deus.paperwork.entities.employee.model.EmployeeHudState;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The pure helpers that translate the synced data.
///
/// The mob itself cannot be built in a test (it needs a BTA world with its
/// registry bootstrap), but the decoding of the HUD bytes and the comparison
/// of two stacks are pure and are the parts that can break silently.
class MobEmployeeSyncDataTest {

	/// The neutral ordinals of the icons the tests do not care about.
	private static final byte GOOD_STATE    = (byte) EmployeeStateIcons.GOOD.ordinal();
	private static final byte GOOD_ALERT    = (byte) EmployeeAlert.GOOD.ordinal();
	private static final byte HAPPY_EMOTION = (byte) EmployeeEmotions.HAPPY.ordinal();

	@BeforeAll
	static void initItems() {
		Items.init();
	}

	@Test
	@DisplayName("every alert ordinal survives the round trip through the synced byte")
	void alertOrdinalsRoundTrip() {
		for (EmployeeAlert alert : EmployeeAlert.values()) {
			assertEquals(alert, decode((byte) alert.ordinal(), GOOD_STATE, HAPPY_EMOTION).alert());
		}
	}

	@Test
	@DisplayName("every state ordinal survives the round trip through the synced byte")
	void stateOrdinalsRoundTrip() {
		for (EmployeeStateIcons state : EmployeeStateIcons.values()) {
			assertEquals(state, decode(GOOD_ALERT, (byte) state.ordinal(), HAPPY_EMOTION).state());
		}
	}

	@Test
	@DisplayName("every emotion ordinal survives the round trip through the synced byte")
	void emotionOrdinalsRoundTrip() {
		for (EmployeeEmotions emotion : EmployeeEmotions.values()) {
			assertEquals(emotion, decode(GOOD_ALERT, GOOD_STATE, (byte) emotion.ordinal()).emotion());
		}
	}

	@Test
	@DisplayName("an alert out of range falls back to GOOD")
	void alertOutOfRangeIsGood() {
		assertEquals(EmployeeAlert.GOOD, decode((byte) 99, GOOD_STATE, HAPPY_EMOTION).alert());
		assertEquals(EmployeeAlert.GOOD, decode((byte) -1, GOOD_STATE, HAPPY_EMOTION).alert());
	}

	@Test
	@DisplayName("a state out of range falls back to GOOD")
	void stateOutOfRangeIsGood() {
		assertEquals(EmployeeStateIcons.GOOD, decode(GOOD_ALERT, (byte) 99, HAPPY_EMOTION).state());
		assertEquals(EmployeeStateIcons.GOOD, decode(GOOD_ALERT, (byte) -1, HAPPY_EMOTION).state());
	}

	@Test
	@DisplayName("an emotion out of range falls back to HAPPY")
	void emotionOutOfRangeIsHappy() {
		assertEquals(EmployeeEmotions.HAPPY, decode(GOOD_ALERT, GOOD_STATE, (byte) 99).emotion());
		assertEquals(EmployeeEmotions.HAPPY, decode(GOOD_ALERT, GOOD_STATE, (byte) -1).emotion());
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

	/// The HUD state the client decodes out of the synced bytes.
	private static EmployeeHudState decode(byte alert, byte state, byte emotion) {
		return EmployeeHudState.fromOrdinals(alert, state, emotion, (short) 0);
	}
}
