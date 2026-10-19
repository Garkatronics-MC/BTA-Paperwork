package deus.paperwork.item;

import deus.paperwork.Paperwork;
import deus.paperwork.entities.employee.MobEmployee;
import deus.paperwork.entities.employee.farmer.FarmerJobState;
import deus.paperwork.entities.employee.job.EmployeeJobs;
import deus.paperwork.util.PoscArea;
import deus.utils.annotations.RegisterItemModel;
import net.minecraft.client.render.item.model.ItemModelStandard;
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
public class FarmStick extends Item {

    private TilePosc pointA = null;
    private TilePosc pointB = null;

    public FarmStick(@NotNull String translationKey, @NotNull String namespaceId, int id) {
        super(translationKey, namespaceId, id);
    }

    @Override
    public boolean onUseOnBlock(@NotNull ItemStack selfStack, @NotNull World world,
                                @Nullable Player player, @NotNull TilePosc blockPos,
                                @NotNull Side side, double xHit, double yHit) {
        if (world.isClientSide || player == null) return false;

        TilePosc pos = new TilePos(blockPos.x(), blockPos.y(), blockPos.z());

        if (player.isSneaking()) {
            pointB = pos;
            Paperwork.LOGGER.info("[FarmStick] Point B set: {}", pos);
            say(player, TextFormatting.Base.YELLOW, "Farm area: point B set " + fmt(pos) + ".");
        } else {
            pointA = pos;
            pointB = null; // reset B on new A
            Paperwork.LOGGER.info("[FarmStick] Point A set: {}", pos);
            say(player, TextFormatting.Base.YELLOW, "Farm area: point A set " + fmt(pos)
                + " — sneak+click the opposite corner to finish the area.");
        }

        if (pointA != null && pointB != null) {
            Paperwork.LOGGER.info("[FarmStick] Area ready: {} -> {}", pointA, pointB);
            say(player, TextFormatting.Base.GREEN, "Farm area ready — right-click a farmer to assign it.");
        }

        return true;
    }

    @Override
    public boolean useOnEntity(@NotNull ItemStack selfStack, @NotNull Player player, @NotNull Mob mob) {
        if (!(mob instanceof MobEmployee employee)) return false;

        if (pointA == null || pointB == null) {
            Paperwork.LOGGER.info("[FarmStick] Area incomplete — set both points first (click A, shift+click B)");
            if (!player.world.isClientSide) {
                say(player, TextFormatting.Base.YELLOW, "Farm area incomplete — set both corners first (click A, sneak+click B).");
            }
            return false;
        }

        employee.setJob(EmployeeJobs.FARMER_ID);
        employee.setJobState(FarmerJobState.withFarmArea(new PoscArea.Area2D(pointA, pointB)));
        Paperwork.LOGGER.info("[FarmStick] Farm area assigned to farmer: {} -> {}", pointA, pointB);

        if (!player.world.isClientSide) {
            say(player, TextFormatting.Base.GREEN, "Farmer got a farm area " + fmt(pointA) + " -> " + fmt(pointB) + ".");
        }

        pointA = null;
        pointB = null;
        return true;
    }

    private static String fmt(TilePosc pos) {
        return pos.x() + "/" + pos.y() + "/" + pos.z();
    }

    private void say(Player player, TextFormatting.Base color, String message) {
        player.sendMessage(color, "[Farm] " + message);
    }
}
