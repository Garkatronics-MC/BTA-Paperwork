package deus.paperwork.item;

import deus.brainless.Brainless;
import deus.paperwork.entities.employee.MobEmployee;
import deus.paperwork.util.PoscArea;
import deus.utils.annotations.RegisterItemModel;
import net.minecraft.client.render.item.model.ItemModelStandard;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.net.command.TextFormatting;
import net.minecraft.core.util.helper.Side;
import net.minecraft.core.world.World;
import net.minecraft.core.world.pos.TilePos;
import net.minecraft.core.world.pos.TilePosc;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RegisterItemModel(model = ItemModelStandard.class)
public class EmployeeStick extends Item {

	public enum AssignMode { BED, FOOD }

	private TilePosc pendingBed  = null;
	private TilePosc foodPointA  = null;
	private TilePosc foodPointB  = null;

	public EmployeeStick(@NotNull String translationKey, @NotNull String namespaceId, int id) {
		super(translationKey, namespaceId, id);
	}

	@Override
	public boolean onUseOnBlock(@NotNull ItemStack selfStack, @NotNull World world,
	                            @Nullable Player player, @NotNull TilePosc blockPos,
	                            @NotNull Side side, double xHit, double yHit) {
		if (world.isClientSide || player == null) return false;

		int blockId = world.getBlockType(blockPos).id();
		TilePosc pos = new TilePos(blockPos.x(), blockPos.y(), blockPos.z());

		if (blockId == Blocks.BED.id()) {
			pendingBed = pos;
			Brainless.LOGGER.info("[Stick] Bed position saved: {}", pos);
			say(player, TextFormatting.Base.YELLOW, "Employee stick: bed position saved " + fmt(pos)
				+ " — now right-click an employee to hand it over.");

		} else if (blockId == Blocks.LOG_OAK.id()) {
			if (player.isSneaking()) {
				foodPointB = pos;
				Brainless.LOGGER.info("[Stick] Food point B set: {}", pos);
				say(player, TextFormatting.Base.YELLOW, "Food area: point B set " + fmt(pos) + ".");
				if (foodPointA != null) {
					Brainless.LOGGER.info("[Stick] Food area ready: {} -> {}", foodPointA, foodPointB);
					say(player, TextFormatting.Base.GREEN, "Food area ready — right-click an employee to assign it.");
				}
			} else {
				foodPointA = pos;
				foodPointB = null;
				Brainless.LOGGER.info("[Stick] Food point A set: {}", pos);
				say(player, TextFormatting.Base.YELLOW, "Food area: point A set " + fmt(pos)
					+ " — sneak+click the second post to finish the area.");
			}
		} else {
			Brainless.LOGGER.info("[Stick] Block not recognized for assignment");
			say(player, TextFormatting.Base.RED, "Employee stick: click a bed or the posts of your food area.");
			return false;
		}

		return true;
	}

	@Override
	public boolean useOnEntity(@NotNull ItemStack selfStack, @NotNull Player player, @NotNull Mob mob) {
		if (!(mob instanceof MobEmployee employee)) return false;

		boolean assigned = false;

		if (pendingBed != null) {
			employee.assignBed(pendingBed);
			Brainless.LOGGER.info("[Stick] Bed assigned to employee at {}", pendingBed);
			if (!player.world.isClientSide) {
				say(player, TextFormatting.Base.GREEN, "Employee got a bed at " + fmt(pendingBed) + ".");
			}
			pendingBed = null;
			assigned = true;
		}

		if (foodPointA != null && foodPointB != null) {
			employee.assignFoodPlace(new PoscArea.Area2D(foodPointA, foodPointB));
			Brainless.LOGGER.info("[Stick] Food area assigned to employee: {} -> {}", foodPointA, foodPointB);
			if (!player.world.isClientSide) {
				say(player, TextFormatting.Base.GREEN, "Employee got a food area " + fmt(foodPointA) + " -> " + fmt(foodPointB) + ".");
			}
			foodPointA = null;
			foodPointB = null;
			assigned = true;
		} else if (foodPointA != null) {
			Brainless.LOGGER.info("[Stick] Food area incomplete — shift+click second chest for point B");
			say(player, TextFormatting.Base.YELLOW, "Food area incomplete — sneak+click the second post of the area first.");
		}

		if (!assigned) {
			Brainless.LOGGER.info("[Stick] Nothing to assign — save a position first");
			say(player, TextFormatting.Base.RED, "Nothing to assign — save a bed or food area with the stick first.");
		}

		return assigned;
	}

	private static String fmt(TilePosc pos) {
		return pos.x() + "/" + pos.y() + "/" + pos.z();
	}

	private void say(Player player, TextFormatting.Base color, String message) {
		player.sendMessage(color, "[Employee] " + message);
	}
}
