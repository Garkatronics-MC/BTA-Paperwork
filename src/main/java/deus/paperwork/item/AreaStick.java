package deus.paperwork.item;

import deus.brainless.Brainless;
import deus.paperwork.entity.employee.MobEmployee;
import deus.paperwork.core.util.PoscArea;
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

/// Base class for the zone-assignment sticks: work area, rest area, home
/// area, and food area.
///
/// Each stick lets the player mark two corners (point A with a normal
/// right-click on a block, point B with a sneak+right-click) and then assign
/// the resulting area to an employee by right-clicking the employee.
abstract class AreaStick extends Item {

	/// The block the player must click to mark points. Subclasses override
	/// this to restrict which block type is accepted.
	protected abstract boolean isMarkBlock(int blockId);

	/// Message shown when the player clicks a block that is not the mark block.
	protected abstract String invalidBlockMessage();

	/// Assigns the marked area to the employee. Subclasses implement the
	/// actual assignment (workArea, restArea, ...).
	protected abstract void assignArea(MobEmployee employee, PoscArea.Area2D area);

	/// Message shown when the area is successfully assigned.
	protected abstract String assignedMessage(PoscArea.Area2D area);

	/// Optional report about what was found inside the assigned area.
	/// Returns null when there is nothing to report.
	protected String areaReport(MobEmployee employee, PoscArea.Area2D area) {
		return null;
	}

	/// Message shown when the employee cannot receive this area (e.g. wrong
	/// job). Null means no special message.
	protected abstract String invalidEmployeeMessage(MobEmployee employee);

	/// Name used in log messages.
	protected abstract String logName();

	protected TilePosc pointA = null;
	protected TilePosc pointB = null;

	protected AreaStick(@NotNull String translationKey, @NotNull String namespaceId, int id) {
		super(translationKey, namespaceId, id);
	}

	@Override
	public boolean onUseOnBlock(@NotNull ItemStack selfStack, @NotNull World world,
	                            @Nullable Player player, @NotNull TilePosc blockPos,
	                            @NotNull Side side, double xHit, double yHit) {
		if (world.isClientSide || player == null) return false;

		int blockId = world.getBlockType(blockPos).id();
		TilePosc pos = new TilePos(blockPos.x(), blockPos.y(), blockPos.z());

		if (!isMarkBlock(blockId)) {
			Brainless.LOGGER.info("[{}] Block not recognized for assignment", logName());
			say(player, TextFormatting.Base.RED, invalidBlockMessage());
			return false;
		}

		if (player.isSneaking()) {
			pointB = pos;
			Brainless.LOGGER.info("[{}] Point B set: {}", logName(), pos);
			say(player, TextFormatting.Base.YELLOW, logName() + ": point B set " + fmt(pos) + ".");
			if (pointA != null) {
				Brainless.LOGGER.info("[{}] Area ready: {} -> {}", logName(), pointA, pointB);
				say(player, TextFormatting.Base.GREEN, logName() + " area ready — right-click an employee to assign it.");
			}
		} else {
			pointA = pos;
			pointB = null;
			Brainless.LOGGER.info("[{}] Point A set: {}", logName(), pos);
			say(player, TextFormatting.Base.YELLOW, logName() + ": point A set " + fmt(pos)
				+ " — sneak+click the second block to finish the area.");
		}
		return true;
	}

	@Override
	public boolean useOnEntity(@NotNull ItemStack selfStack, @NotNull Player player, @NotNull Mob mob) {
		if (!(mob instanceof MobEmployee employee)) return false;

		if (pointA == null || pointB == null) {
			Brainless.LOGGER.info("[{}] Area incomplete — set both points first", logName());
			if (!player.world.isClientSide) {
				say(player, TextFormatting.Base.YELLOW, logName() + " area incomplete — set both corners first (click A, sneak+click B).");
			}
			return false;
		}

		String invalid = invalidEmployeeMessage(employee);
		if (invalid != null) {
			Brainless.LOGGER.info("[{}] {}", logName(), invalid);
			if (!player.world.isClientSide) {
				say(player, TextFormatting.Base.RED, invalid);
			}
			pointA = null;
			pointB = null;
			return true;
		}

		PoscArea.Area2D area = new PoscArea.Area2D(pointA, pointB);
		assignArea(employee, area);
		Brainless.LOGGER.info("[{}] Area assigned: {} -> {}", logName(), pointA, pointB);
		if (!player.world.isClientSide) {
			say(player, TextFormatting.Base.GREEN, assignedMessage(area));
			String report = areaReport(employee, area);
			if (report != null && !report.isBlank()) {
				say(player, TextFormatting.Base.GREEN, report);
			}
		}

		pointA = null;
		pointB = null;
		return true;
	}

	protected static String fmt(TilePosc pos) {
		return pos.x() + "/" + pos.y() + "/" + pos.z();
	}

	protected void say(Player player, TextFormatting.Base color, String message) {
		if (player != null) {
			player.sendMessage(color, "[Employee] " + message);
		}
	}
}
