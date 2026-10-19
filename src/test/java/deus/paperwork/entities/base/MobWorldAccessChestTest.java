package deus.paperwork.entities.base;

import net.minecraft.core.block.entity.TileEntityChest;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/// Taking seeds from a chest into the inventory of an employee: the chest
/// keeps what does not fit.
class MobWorldAccessChestTest {

	@BeforeAll
	static void initItems() {
		// The item registry has to be filled before a stack can be built.
		Items.init();
	}

	@Test
	@DisplayName("the seeds of a chest go to the inventory")
	void theSeedsGoToTheInventory() {
		ContainerMob inventory = new ContainerMob("test_inventory", 9);
		TileEntityChest chest = chestWith(new ItemStack(Items.SEEDS_WHEAT, 30));

		int taken = MobWorldAccess.takeFromChest(inventory, chest, Items.SEEDS_WHEAT, 16);

		assertEquals(16, taken);
		assertEquals(16, inventory.findStackOf(Items.SEEDS_WHEAT).stackSize);
		assertEquals(14, chest.getItem(0).stackSize);
	}

	@Test
	@DisplayName("it never takes more than the chest has")
	void itNeverTakesMoreThanTheChestHas() {
		ContainerMob inventory = new ContainerMob("test_inventory", 9);
		TileEntityChest chest = chestWith(new ItemStack(Items.SEEDS_WHEAT, 5));

		int taken = MobWorldAccess.takeFromChest(inventory, chest, Items.SEEDS_WHEAT, 16);

		assertEquals(5, taken);
		assertEquals(5, inventory.findStackOf(Items.SEEDS_WHEAT).stackSize);
		assertNull(chest.getItem(0), "the empty slot of the chest is cleared");
	}

	@Test
	@DisplayName("a full inventory leaves the chest alone")
	void aFullInventoryLeavesTheChestAlone() {
		ContainerMob inventory = new ContainerMob("test_inventory", 1);
		inventory.insertItem(new ItemStack(Items.WHEAT, 64));
		TileEntityChest chest = chestWith(new ItemStack(Items.SEEDS_WHEAT, 30));

		int taken = MobWorldAccess.takeFromChest(inventory, chest, Items.SEEDS_WHEAT, 16);

		assertEquals(0, taken);
		assertEquals(30, chest.getItem(0).stackSize);
	}

	@Test
	@DisplayName("a chest without seeds gives nothing")
	void aChestWithoutSeedsGivesNothing() {
		ContainerMob inventory = new ContainerMob("test_inventory", 9);
		TileEntityChest chest = chestWith(new ItemStack(Items.WHEAT, 30));

		int taken = MobWorldAccess.takeFromChest(inventory, chest, Items.SEEDS_WHEAT, 16);

		assertEquals(0, taken);
		assertNull(inventory.findStackOf(Items.SEEDS_WHEAT));
	}

	// ------------------------------------------------------------------ helpers

	/// A chest with one stack in its first slot.
	private static TileEntityChest chestWith(ItemStack stack) {
		TileEntityChest chest = new TileEntityChest();
		assertNotNull(chest);
		chest.setItem(0, stack);
		return chest;
	}
}
