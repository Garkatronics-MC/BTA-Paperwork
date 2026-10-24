package deus.paperwork.item;

import deus.brainless.Brainless;
import deus.paperwork.entity.base.MobWorldAccess;
import deus.paperwork.entity.employee.MobEmployee;
import deus.paperwork.core.util.PoscArea;
import deus.utils.annotations.RegisterItemModel;
import net.minecraft.client.render.item.model.ItemModelStandard;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.net.command.TextFormatting;
import net.minecraft.core.util.helper.Side;
import net.minecraft.core.world.World;
import net.minecraft.core.world.pos.TilePosc;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// Right-click a block to mark point A, sneak+right-click another block to
/// mark point B, then right-click an employee to assign the area as its home
/// zone. The home zone must contain a bed.
@RegisterItemModel(model = ItemModelStandard.class)
public class HomeAreaStick extends AreaStick {

	public HomeAreaStick(@NotNull String translationKey, @NotNull String namespaceId, int id) {
		super(translationKey, namespaceId, id);
	}

	@Override
	protected boolean isMarkBlock(int blockId) {
		return true;
	}

	@Override
	protected String invalidBlockMessage() {
		return "Employee stick: click any block to mark a home area corner.";
	}

	@Override
	protected void assignArea(MobEmployee employee, PoscArea.Area2D area) {
		employee.assignHomeArea(area);
	}

	@Override
	protected String assignedMessage(PoscArea.Area2D area) {
		return "Employee got a home area " + fmt(area.a()) + " -> " + fmt(area.b()) + ".";
	}

	@Override
	protected String invalidEmployeeMessage(MobEmployee employee) {
		return null;
	}

	@Override
	protected String logName() {
		return "HomeAreaStick";
	}

	/// Reports whether a bed was found inside the assigned home area.
	@Override
	protected String areaReport(MobEmployee employee, PoscArea.Area2D area) {
		boolean hasBed = MobWorldAccess.getBlocksInArea3D(area).stream()
			.anyMatch(p -> employee.world.getBlockType(p).id() == Blocks.BED.id());
		return "[Home area check] " + (hasBed ? "OK: bed found." : "MISSING bed.");
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

		PoscArea.Area2D area = new PoscArea.Area2D(pointA, pointB);

		// Home zone must contain a bed.
		boolean hasBed = MobWorldAccess.getBlocksInArea3D(area).stream()
			.anyMatch(p -> mob.world.getBlockType(p).id() == Blocks.BED.id());
		if (!hasBed) {
			if (!player.world.isClientSide) {
				say(player, TextFormatting.Base.RED, "[Employee] Home zone must contain a bed.");
			}
			pointA = null;
			pointB = null;
			return true;
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
}
