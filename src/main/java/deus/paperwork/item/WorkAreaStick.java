package deus.paperwork.item;

import deus.brainless.Brainless;
import deus.paperwork.entity.base.MobWorldAccess;
import deus.paperwork.entity.employee.MobEmployee;
import deus.paperwork.entity.employee.chef.CookTask;
import deus.paperwork.core.util.PoscArea;
import deus.utils.annotations.RegisterItemModel;
import net.minecraft.client.render.item.model.ItemModelStandard;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.entity.TileEntityChest;
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
/// mark point B, then right-click an employee to assign the area as its work
/// zone.
@RegisterItemModel(model = ItemModelStandard.class)
public class WorkAreaStick extends AreaStick {

	public WorkAreaStick(@NotNull String translationKey, @NotNull String namespaceId, int id) {
		super(translationKey, namespaceId, id);
	}

	@Override
	protected boolean isMarkBlock(int blockId) {
		return true; // any block can mark a corner
	}

	@Override
	protected String invalidBlockMessage() {
		return "Employee stick: click any block to mark a work area corner.";
	}

	@Override
	protected void assignArea(MobEmployee employee, PoscArea.Area2D area) {
		employee.assignWorkArea(area);
	}

	@Override
	protected String assignedMessage(PoscArea.Area2D area) {
		return "Employee got a work area " + fmt(area.a()) + " -> " + fmt(area.b()) + ".";
	}

	@Override
	protected String invalidEmployeeMessage(MobEmployee employee) {
		return null; // any employee can have a work area
	}

	@Override
	protected String logName() {
		return "WorkAreaStick";
	}

	/// Reports what work-related blocks were found inside the assigned area.
	@Override
	protected String areaReport(MobEmployee employee, PoscArea.Area2D area) {
		String jobId = employee.jobId();
		StringBuilder sb = new StringBuilder();

		if (jobId.equals("chef")) {
			int width = Math.abs(area.a().x() - area.b().x()) + 1;
			int length = Math.abs(area.a().z() - area.b().z()) + 1;
			boolean sizeOk = CookTask.isValidSize(area);
			boolean hasCookStation = false;
			boolean hasChest = false;
			for (TilePosc p : MobWorldAccess.getBlocksInArea3D(area)) {
				int id = employee.world.getBlockType(p).id();
				if (id == Blocks.FURNACE_STONE_IDLE.id()
					|| id == Blocks.FURNACE_STONE_ACTIVE.id()
					|| id == Blocks.WORKBENCH.id()) {
					hasCookStation = true;
				}
				if (MobWorldAccess.getChest(employee.world, p) != null) {
					hasChest = true;
				}
				if (hasCookStation && hasChest) break;
			}
			sb.append("[Work area check] ").append(width).append("x").append(length).append(": ");
			if (!sizeOk) sb.append("INVALID size (need 3x6..16x16); ");
			if (!hasCookStation) sb.append("MISSING furnace/workbench; ");
			if (!hasChest) sb.append("MISSING chest; ");
			if (sizeOk && hasCookStation && hasChest) sb.append("OK: furnace/workbench + chest found.");
			if (sb.toString().endsWith("; ")) {
				sb.setLength(sb.length() - 2);
			}
			return sb.toString();
		} else if (jobId.equals("farmer")) {
			boolean hasFarmland = MobWorldAccess.getBlocksInArea3D(area).stream()
				.anyMatch(p -> employee.world.getBlockType(p).id() == Blocks.FARMLAND_DIRT.id());
			return "[Work area check] " + (hasFarmland ? "OK: farmland found." : "MISSING farmland.");
		}

		return null; // plain employee: nothing specific to report
	}
}
