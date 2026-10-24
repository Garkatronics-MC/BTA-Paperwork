package deus.paperwork.item;

import deus.paperwork.entity.employee.MobEmployee;
import deus.paperwork.core.util.PoscArea;
import deus.utils.annotations.RegisterItemModel;
import net.minecraft.client.render.item.model.ItemModelStandard;
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
/// mark point B, then right-click an employee to assign the area as its food
/// zone.
@RegisterItemModel(model = ItemModelStandard.class)
public class FoodAreaStick extends AreaStick {

	public FoodAreaStick(@NotNull String translationKey, @NotNull String namespaceId, int id) {
		super(translationKey, namespaceId, id);
	}

	@Override
	protected boolean isMarkBlock(int blockId) {
		return true;
	}

	@Override
	protected String invalidBlockMessage() {
		return "Employee stick: click any block to mark a food area corner.";
	}

	@Override
	protected void assignArea(MobEmployee employee, PoscArea.Area2D area) {
		employee.assignFoodArea(area);
	}

	@Override
	protected String assignedMessage(PoscArea.Area2D area) {
		return "Employee got a food area " + fmt(area.a()) + " -> " + fmt(area.b()) + ".";
	}

	@Override
	protected String invalidEmployeeMessage(MobEmployee employee) {
		return null;
	}

	@Override
	protected String logName() {
		return "FoodAreaStick";
	}
}
