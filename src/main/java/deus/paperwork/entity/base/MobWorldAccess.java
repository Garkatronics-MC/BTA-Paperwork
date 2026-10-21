package deus.paperwork.entity.base;

import deus.paperwork.core.util.PoscArea;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.entity.TileEntityChest;
import net.minecraft.core.entity.EntityItem;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import net.minecraft.core.world.pos.TilePos;
import net.minecraft.core.world.pos.TilePosc;
import org.jetbrains.annotations.Nullable;
import org.joml.primitives.AABBd;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/// The way a mob touches the world: the tiles around it, the containers it can
/// reach, and the items lying on the floor it can absorb. Named after the
/// domain, not the role: any mob that holds an inventory can use it.
///
/// A stateless utility, not an entity component: every call passes the world
/// (and the mob / inventory where needed), so it works for any mob without a
/// per-entity instance. The caller that wires mob/inventory is the task driving
/// the work.
public final class MobWorldAccess {

	private MobWorldAccess() {}

	public static List<TilePosc> getBlocksInArea(PoscArea.Area2D blockArea) {
		return PoscArea.tiles(blockArea);
	}

	public static List<TilePosc> getBlocksInArea(World world, PoscArea.Area2D blockArea, int id) {
		return PoscArea.tiles(blockArea).stream()
			.filter(p -> world.getBlockType(p).id() == id)
			.toList();
	}

	public static boolean openChest(World world, TilePosc posc) {
		Block<?> block = world.getBlockType(posc);
		if (!block.isEntityTile() || block.id() != Blocks.CHEST_PLANKS_OAK.id()) return false;
		TileEntityChest chest = (TileEntityChest) world.getTileEntity(posc);

		return true;
	}

	/// Absorbs the items of the given kinds that are lying on the floor around
	/// the mob, into its inventory.
	///
	/// The items that do not fit stay on the floor.
	public static void absorbNearbyItems(Mob mob, ContainerMob inventory, Item... items) {

		AABBd box = MathHelper.aabbGrow(
			mob.bb,
			3.0,
			1.0,
			3.0,
			new AABBd()
		);

		for (EntityItem entityItem :
			mob.world.getEntitiesWithinAABB(EntityItem.class, box)) {

			if (!entityItem.isAlive()) {
				continue;
			}

			if (entityItem.pickupDelay > 0) {
				continue;
			}

			ItemStack stack = entityItem.item;

			if (stack == null) {
				continue;
			}

			if (!isOneOf(stack.getItem(), items)) {
				continue;
			}

			int before = stack.stackSize;

			inventory.insertItem(stack);

			if (stack.stackSize < before) {

				mob.world.playSoundAtEntity(
					null,
					mob,
					"item.pickup",
					0.2F,
					0.2F
				);

				if (stack.stackSize <= 0) {
					entityItem.remove();
				}
			}
		}
	}

	/// Whether an item is one of the given ones.
	private static boolean isOneOf(Item item, Item... items) {
		for (Item candidate : items) {
			if (item.equals(candidate)) {
				return true;
			}
		}
		return false;
	}

	/// Takes up to `amount` items of a kind from a chest into an inventory.
	/// Returns how many were taken.
	///
	/// The chest keeps what does not fit in the inventory.
	public static int takeFromChest(ContainerMob inventory, TileEntityChest chest, Item item, int amount) {
		if (inventory == null || chest == null || item == null || amount <= 0) {
			return 0;
		}

		for (int i = 0; i < chest.getContainerSize(); i++) {
			ItemStack stack = chest.getItem(i);

			if (stack == null || stack.stackSize <= 0) {
				continue;
			}

			if (!stack.getItem().equals(item)) {
				continue;
			}

			int wanted = Math.min(amount, stack.stackSize);
			ItemStack part = stack.copy();
			part.stackSize = wanted;

			inventory.insertItem(part);

			// insertItem leaves what does not fit in the stack it is given.
			int taken = wanted - part.stackSize;

			if (taken <= 0) {
				return 0;
			}

			stack.stackSize -= taken;

			if (stack.stackSize <= 0) {
				chest.setItem(i, null);
			}

			return taken;
		}

		return 0;
	}

	@Nullable
	public static TilePos roamRandomPath(World world, PoscArea.Area2D area, Random rng) {

		int minX = Math.min(area.a().x(), area.b().x());
		int maxX = Math.max(area.a().x(), area.b().x());
		int minZ = Math.min(area.a().z(), area.b().z());
		int maxZ = Math.max(area.a().z(), area.b().z());

		int y = area.a().y();

		for (int i = 0; i < 10; i++) {

			int x = minX + rng.nextInt(maxX - minX + 1);
			int z = minZ + rng.nextInt(maxZ - minZ + 1);

			if (!world.isAirBlock(x, y, z)) {
				continue;
			}

			if (world.isAirBlock(x, y - 1, z)) {
				continue;
			}

			return new TilePos(x, y, z);
		}

		return null;
	}

	public static Optional<ItemStack> findItem(TileEntityChest chest, int itemId) {
		if (chest == null) return Optional.empty();
		for (int i = 0; i < 27; i++) {
			ItemStack stack = chest.getItem(i);
			if (stack != null && stack.getItem().id == itemId) return Optional.of(stack);
		}
		return Optional.empty();
	}

	public static TileEntityChest getChest(World world, TilePosc posc) {
		Block<?> block = world.getBlockType(posc);
		if (!block.isEntityTile() || (block.id() != Blocks.CHEST_PLANKS_OAK.id() && block.id() != Blocks.CHEST_PLANKS_OAK_PAINTED.id())) return null;
		return (TileEntityChest) world.getTileEntity(posc);
	}
}
