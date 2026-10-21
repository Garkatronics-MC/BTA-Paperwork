package deus.paperwork.item;

import deus.paperwork.entity.employee.MobEmployee;
import deus.paperwork.entity.employee.job.EmployeeJob;
import deus.paperwork.entity.employee.job.EmployeeJobs;
import deus.utils.annotations.RegisterItemModel;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.net.command.TextFormatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/// Right-clicking an employee cycles it through every registered job, in
/// registration order: the next job after the current one, wrapping around
/// when it passes the last one. No UI, no state: one stick, one click, one job.
@RegisterItemModel(model = JobStickModel.class)
public class JobStick extends Item {

	public JobStick(@NotNull String translationKey, @NotNull String namespaceId, int id) {
		super(translationKey, namespaceId, id);
	}

	@Override
	public boolean useOnEntity(@NotNull ItemStack selfStack, @NotNull Player player, @NotNull Mob mob) {
		if (!(mob instanceof MobEmployee employee)) return false;

		ArrayList<EmployeeJob> jobs = new ArrayList<>(EmployeeJobs.all());
		if (jobs.isEmpty()) return false;

		// The index of the current job in the registry. An unknown id (it
		// should never happen) starts the cycle over from the first job.
		int current = jobs.size() - 1;
		for (int i = 0; i < jobs.size(); i++) {
			if (jobs.get(i).id().equals(employee.jobId())) {
				current = i;
				break;
			}
		}

		EmployeeJob next = jobs.get((current + 1) % jobs.size());
		employee.setJob(next.id());
		if (!player.world.isClientSide) {
			say(player, TextFormatting.Base.GREEN, "Employee is now a \"" + next.id() + "\" employee"
				+ (current + 1 == jobs.size() ? " (back to the first job)." : "."));
		}
		return true;
	}

	private void say(Player player, TextFormatting.Base color, String message) {
		player.sendMessage(color, "[Job] " + message);
	}
}
