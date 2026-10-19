package deus.paperwork.entities.base;

import deus.paperwork.entities.base.ContainerMob;
import deus.paperwork.util.PoscArea;
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

public class MobWorldAccess {

	protected Mob mob;
	protected ContainerMob inventory;

	public MobWorldAccess(Mob mob, ContainerMob inventory) {
		this.mob = mob;
		this.inventory = inventory;
	}


	public List<TilePosc> getBlocksInArea(PoscArea.Area2D blockArea) {
		return PoscArea.tiles(blockArea);
	}

	public List<TilePosc> getBlocksInArea(PoscArea.Area2D blockArea, int id) {
		return getBlocksInArea(this.mob.world, blockArea, id);
	}

	/// Static so any mob can scan with a world in hand, no instance needed —
	/// used directly by {@link deus.paperwork.entities.base.CropWorldAccess}.
	public static List<TilePosc> getBlocksInArea(World world, PoscArea.Area2D blockArea, int id) {
		return PoscArea.tiles(blockArea).stream()
			.filter(p -> world.getBlockType(p).id() == id)
			.toList();
	}

	public boolean openChest(TilePosc posc) {
		Block<?> block = this.mob.world.getBlockType(posc);
		if (!block.isEntityTile() || block.id() != Blocks.CHEST_PLANKS_OAK.id()) return false;
		TileEntityChest chest = (TileEntityChest) this.mob.world.getTileEntity(posc);

		return true;
	}

	public void absorbNearbyItems(Item item) {

		AABBd box = MathHelper.aabbGrow(
			this.mob.bb,
			3.0,
			1.0,
			3.0,
			new AABBd()
		);

		for (EntityItem entityItem :
			this.mob.world.getEntitiesWithinAABB(EntityItem.class, box)) {

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

			if (!stack.getItem().equals(item)) {
				continue;
			}

			int before = stack.stackSize;

			this.inventory.insertItem(stack);

			if (stack.stackSize < before) {

				this.mob.world.playSoundAtEntity(
					null,
					this.mob,
					"item.pickup",
					0.2F,
					1.0F
				);

				if (stack.stackSize <= 0) {
					entityItem.remove();
				}
			}
		}
	}

	@Nullable
	public TilePos roamRandomPath(PoscArea.Area2D area, Random rng) {

		int minX = Math.min(area.a().x(), area.b().x());
		int maxX = Math.max(area.a().x(), area.b().x());
		int minZ = Math.min(area.a().z(), area.b().z());
		int maxZ = Math.max(area.a().z(), area.b().z());

		int y = area.a().y();

		for (int i = 0; i < 10; i++) {

			int x = minX + rng.nextInt(maxX - minX + 1);
			int z = minZ + rng.nextInt(maxZ - minZ + 1);

			if (!this.mob.world.isAirBlock(x, y, z)) {
				continue;
			}

			if (this.mob.world.isAirBlock(x, y - 1, z)) {
				continue;
			}

			return new TilePos(x, y, z);
		}

		return null;
	}

	public Optional<ItemStack> findItem(TileEntityChest chest, int itemId) {
		if (chest == null) return Optional.empty();
		for (int i = 0; i < 27; i++) {
			ItemStack stack = chest.getItem(i);
			if (stack != null && stack.getItem().id == itemId) return Optional.of(stack);
		}
		return Optional.empty();
	}

	public TileEntityChest getChest(TilePosc posc) {
		Block<?> block = this.mob.world.getBlockType(posc);
		if (!block.isEntityTile() || (block.id() != Blocks.CHEST_PLANKS_OAK.id() && block.id() != Blocks.CHEST_PLANKS_OAK_PAINTED.id())) return null;
		return (TileEntityChest) this.mob.world.getTileEntity(posc);
	}
}

