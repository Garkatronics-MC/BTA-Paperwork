package deus.paperwork.entities.employee.farmer;

import deus.paperwork.Paperwork;
import deus.paperwork.entities.base.MobWorldAccess;
import deus.paperwork.entities.employee.MobEmployee;
import deus.paperwork.util.PoscArea;
import net.minecraft.core.block.entity.TileEntityChest;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.Items;
import net.minecraft.core.world.pos.TilePos;
import net.minecraft.core.world.pos.TilePosc;

import java.util.ArrayList;
import java.util.List;

/// What the farmer gathers while it works: the wheat and the seeds that are on
/// the floor around it, and the seeds it takes from the chests next to it.
///
/// The gathering is the other half of the farming: the harvest drops the crop
/// on the floor and the sowing eats the seeds of the inventory, so without
/// this the farmer would fill the field and never carry anything.
public final class FarmerCollector {

	/// The items the farmer picks up from the floor.
	private static final Item[] GATHERED = {Items.WHEAT, Items.SEEDS_WHEAT};

	/// How many seeds the farmer takes from a chest at once.
	private static final int SEEDS_PER_TRIP = 16;

	/// How often (in ticks) the farmer looks for a chest with seeds.
	private static final int CHEST_SEARCH_INTERVAL = 20;

	/// How far (in blocks) the farmer looks for a chest.
	private static final int CHEST_RADIUS = 2;

	private FarmerCollector() {}

	/// Gathers what is around the farmer.
	public static void collect(MobEmployee mob) {
		if (mob == null) {
			return;
		}
		absorbFromFloor(mob);
		takeSeedsFromChests(mob);
	}

	/// Picks up the wheat and the seeds that are on the floor around the farmer.
	private static void absorbFromFloor(MobEmployee mob) {
		MobWorldAccess.absorbNearbyItems(mob, mob.inventory, GATHERED);
	}

	/// Takes seeds from a chest next to the farmer.
	///
	/// Only when it has no seeds left: the inventory of an employee is small
	/// and the wheat it gathers matters more than a stock of seeds.
	private static void takeSeedsFromChests(MobEmployee mob) {
		boolean hasSeeds = mob.inventory.findStackOf(Items.SEEDS_WHEAT) != null;
		long tick = mob.world.getTotalWorldTime();

		if (!wantsSeeds(hasSeeds, tick)) {
			return;
		}

		for (TilePosc pos : chestsAround(mob)) {
			TileEntityChest chest = MobWorldAccess.getChest(mob.world, pos);

			if (chest == null) {
				continue;
			}

			int taken = MobWorldAccess.takeFromChest(mob.inventory, chest, Items.SEEDS_WHEAT, SEEDS_PER_TRIP);

			if (taken > 0) {
				Paperwork.LOGGER.info("[Farmer] took {} seeds from the chest at {}", taken, pos);
				return;
			}
		}
	}

	/// Whether the farmer should look for seeds in a chest right now.
	///
	/// Pure, so it can be tested without a mob: it looks for seeds only when
	/// it has none, and not on every tick (looking in the chests of the
	/// neighbourhood is not free).
	public static boolean wantsSeeds(boolean hasSeeds, long tick) {
		return !hasSeeds && tick % CHEST_SEARCH_INTERVAL == 0;
	}

	/// The blocks around the farmer where a chest could be: the ones of its
	/// level and the ones just above (a chest stands on the ground).
	private static List<TilePosc> chestsAround(MobEmployee mob) {
		int x = (int) Math.floor(mob.x);
		int y = (int) Math.floor(mob.y);
		int z = (int) Math.floor(mob.z);

		List<TilePosc> positions = new ArrayList<>();

		for (int level = 0; level <= 1; level++) {
			PoscArea.Area2D area = new PoscArea.Area2D(
				new TilePos(x - CHEST_RADIUS, y + level, z - CHEST_RADIUS),
				new TilePos(x + CHEST_RADIUS, y + level, z + CHEST_RADIUS)
			);
			positions.addAll(PoscArea.tiles(area));
		}

		return positions;
	}
}
