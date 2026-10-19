package deus.paperwork.entities.base;

import com.mojang.nbt.tags.CompoundTag;
import com.mojang.nbt.tags.ListTag;
import com.mojang.nbt.tags.Tag;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/// NBT round trip of {@link ContainerMob}.
///
/// The tests use the real item registry (`net.minecraft.core.item.Items`) and real
/// `ItemStack`s. `Items.init()` only fills the static item registry, it does not
/// need a `World` nor a running game, so the tests stay headless.
class ContainerMobNbtTest {

	private static final int SIZE = 9;

	@BeforeAll
	static void initItems() {
		Items.init();
	}

	private static ContainerMob newContainer() {
		return new ContainerMob("test", SIZE);
	}

	private static CompoundTag slotTag(CompoundTag tag, int index) {
		ListTag slots = tag.getList(ContainerMob.SLOTS_KEY);
		Tag<?> entry = slots.tagAt(index);
		assertTrue(entry instanceof CompoundTag, "slot " + index + " is not a compound");
		return (CompoundTag) entry;
	}

	@Test
	@DisplayName("save writes one compound per slot")
	void saveWritesOneCompoundPerSlot() {
		ContainerMob container = newContainer();
		container.setItem(2, new ItemStack(Items.WHEAT, 5));

		CompoundTag tag = new CompoundTag();
		container.saveToNBT(tag);

		ListTag slots = tag.getList(ContainerMob.SLOTS_KEY);
		assertEquals(SIZE, slots.tagCount(), "there must be one entry per slot");
	}

	@Test
	@DisplayName("an empty slot is written as empty, not as a stack of zero")
	void emptySlotIsWrittenAsEmpty() {
		ContainerMob container = newContainer();

		CompoundTag tag = new CompoundTag();
		container.saveToNBT(tag);

		for (int i = 0; i < SIZE; i++) {
			CompoundTag slot = slotTag(tag, i);
			assertTrue(slot.getValues().isEmpty(), "slot " + i + " should be empty");
			assertTrue(!slot.containsKey(ContainerMob.ITEM_KEY),
				"slot " + i + " should not hold an item");
		}
	}

	@Test
	@DisplayName("a filled slot keeps the item id, the amount and the item NBT")
	void filledSlotKeepsItemData() {
		ContainerMob container = newContainer();
		ItemStack wheat = new ItemStack(Items.WHEAT, 5);
		CompoundTag custom = new CompoundTag();
		custom.putString("Owner", "employee");
		wheat.setData(custom);
		container.setItem(4, wheat);

		CompoundTag tag = new CompoundTag();
		container.saveToNBT(tag);

		CompoundTag slot = slotTag(tag, 4);
		assertTrue(slot.containsKey(ContainerMob.ITEM_KEY));

		CompoundTag item = slot.getCompound(ContainerMob.ITEM_KEY);
		assertEquals(wheat.itemID, item.getShort("id"));
		assertEquals(5, item.getByte("Count"));
		assertEquals("employee", item.getCompound("Data").getString("Owner"));
	}

	@Test
	@DisplayName("round trip keeps every stack, including the same item in two slots")
	void roundTripKeepsEveryStack() {
		ContainerMob container = newContainer();
		container.setItem(0, new ItemStack(Items.WHEAT, 5));
		container.setItem(3, new ItemStack(Items.STICK, 2));
		container.setItem(5, new ItemStack(Items.WHEAT, 7));

		CompoundTag tag = new CompoundTag();
		container.saveToNBT(tag);

		ContainerMob loaded = newContainer();
		loaded.loadFromNBT(tag);

		assertStack(loaded, 0, Items.WHEAT, 5);
		assertStack(loaded, 3, Items.STICK, 2);
		assertStack(loaded, 5, Items.WHEAT, 7);
	}

	@Test
	@DisplayName("empty slots stay empty after the round trip")
	void emptySlotsStayEmpty() {
		ContainerMob container = newContainer();
		container.setItem(1, new ItemStack(Items.STICK, 1));

		CompoundTag tag = new CompoundTag();
		container.saveToNBT(tag);

		ContainerMob loaded = newContainer();
		loaded.loadFromNBT(tag);

		for (int i = 0; i < SIZE; i++) {
			if (i == 1) {
				continue;
			}

			assertNull(loaded.getItem(i), "slot " + i + " should be empty");
		}
	}

	@Test
	@DisplayName("the item NBT (custom data) survives the round trip")
	void itemNbtSurvivesTheRoundTrip() {
		ContainerMob container = newContainer();
		ItemStack wheat = new ItemStack(Items.WHEAT, 1);
		CompoundTag custom = new CompoundTag();
		custom.putString("Owner", "employee");
		custom.putInt("Quality", 7);
		wheat.setData(custom);
		container.setItem(0, wheat);

		CompoundTag tag = new CompoundTag();
		container.saveToNBT(tag);

		ContainerMob loaded = newContainer();
		loaded.loadFromNBT(tag);

		ItemStack stack = loaded.getItem(0);
		assertNotNull(stack);
		assertEquals("employee", stack.getData().getString("Owner"));
		assertEquals(7, stack.getData().getInteger("Quality"));
	}

	@Test
	@DisplayName("loading replaces the previous content of the container")
	void loadingReplacesPreviousContent() {
		ContainerMob container = newContainer();
		container.setItem(0, new ItemStack(Items.WHEAT, 5));
		container.setItem(1, new ItemStack(Items.STICK, 2));
		container.setItem(2, new ItemStack(Items.WHEAT, 7));

		ContainerMob source = newContainer();
		source.setItem(6, new ItemStack(Items.STICK, 3));

		CompoundTag tag = new CompoundTag();
		source.saveToNBT(tag);

		container.loadFromNBT(tag);

		assertNull(container.getItem(0), "slot 0 should have been cleared");
		assertNull(container.getItem(1), "slot 1 should have been cleared");
		assertNull(container.getItem(2), "slot 2 should have been cleared");
		assertStack(container, 6, Items.STICK, 3);
	}

	@Test
	@DisplayName("save -> load -> save produces the same tag")
	void saveLoadSaveIsStable() {
		ContainerMob container = newContainer();
		container.setItem(0, new ItemStack(Items.WHEAT, 5));
		container.setItem(4, new ItemStack(Items.STICK, 2));
		container.setItem(4 + 1, new ItemStack(Items.WHEAT, 7));

		ItemStack named = new ItemStack(Items.STICK, 1);
		CompoundTag custom = new CompoundTag();
		custom.putString("Owner", "employee");
		named.setData(custom);
		container.setItem(8, named);

		CompoundTag first = new CompoundTag();
		container.saveToNBT(first);

		ContainerMob loaded = newContainer();
		loaded.loadFromNBT(first);

		CompoundTag second = new CompoundTag();
		loaded.saveToNBT(second);

		assertEquals(first, second, "the tag must be identical after a round trip");
	}

	@Test
	@DisplayName("loading an empty tag does not throw and empties the container")
	void loadingEmptyTagDoesNotThrow() {
		ContainerMob container = newContainer();
		container.setItem(0, new ItemStack(Items.WHEAT, 5));

		assertDoesNotThrow(() -> container.loadFromNBT(new CompoundTag()));

		for (int i = 0; i < SIZE; i++) {
			assertNull(container.getItem(i), "slot " + i + " should be empty");
		}
	}

	@Test
	@DisplayName("loading a tag without slots does not throw")
	void loadingTagWithoutSlotsDoesNotThrow() {
		ContainerMob container = newContainer();
		container.setItem(0, new ItemStack(Items.WHEAT, 5));

		CompoundTag tag = new CompoundTag();
		tag.putString("Unrelated", "value");

		assertDoesNotThrow(() -> container.loadFromNBT(tag));

		assertNull(container.getItem(0), "slot 0 should be empty");
	}

	@Test
	@DisplayName("loading a null tag does not throw and empties the container")
	void loadingNullTagDoesNotThrow() {
		ContainerMob container = newContainer();
		container.setItem(0, new ItemStack(Items.WHEAT, 5));

		assertDoesNotThrow(() -> container.loadFromNBT(null));

		assertNull(container.getItem(0), "slot 0 should be empty");
	}

	@Test
	@DisplayName("a container of a different size only loads the slots it has")
	void loadingIntoASmallerContainerKeepsOnlyItsOwnSlots() {
		ContainerMob source = newContainer();
		source.setItem(0, new ItemStack(Items.WHEAT, 5));
		source.setItem(8, new ItemStack(Items.STICK, 2));

		CompoundTag tag = new CompoundTag();
		source.saveToNBT(tag);

		ContainerMob small = new ContainerMob("small", 2);
		small.loadFromNBT(tag);

		assertStack(small, 0, Items.WHEAT, 5);
		assertNull(small.getItem(1));
	}

	@Test
	@DisplayName("the rest of the container API keeps working after a round trip")
	void otherApiStillWorksAfterARoundTrip() {
		ContainerMob container = newContainer();
		container.setItem(0, new ItemStack(Items.WHEAT, 5));

		CompoundTag tag = new CompoundTag();
		container.saveToNBT(tag);

		ContainerMob loaded = newContainer();
		loaded.loadFromNBT(tag);

		assertTrue(loaded.available(Items.WHEAT));
		assertNull(loaded.findStackOf(Items.STICK));
		assertNotNull(loaded.findStackOf(Items.WHEAT));
		assertEquals(SIZE, loaded.getContainerSize());

		ItemStack extra = new ItemStack(Items.STICK, 4);
		assertEquals(4, loaded.insertItem(extra));
		assertEquals(0, extra.stackSize);
	}

	private static void assertStack(ContainerMob container, int slot, net.minecraft.core.item.Item item, int amount) {
		ItemStack stack = container.getItem(slot);
		assertNotNull(stack, "slot " + slot + " should hold a stack");
		assertEquals(item.id, stack.itemID, "wrong item in slot " + slot);
		assertEquals(amount, stack.stackSize, "wrong amount in slot " + slot);
		assertSame(item, stack.getItem(), "wrong item instance in slot " + slot);
	}
}
