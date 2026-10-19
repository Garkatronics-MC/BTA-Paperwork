package deus.paperwork.entities.employee.farmer;

import com.mojang.nbt.tags.CompoundTag;
import deus.paperwork.entities.employee.job.EmployeeJobState;
import deus.paperwork.util.PoscArea;
import net.minecraft.core.world.pos.TilePos;
import net.minecraft.core.world.pos.TilePosc;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/// The mutable state of the farmer job: the plot the farmer works.
///
/// One instance lives in every farmer (the job itself is a shared singleton),
/// and it is saved with the mob: the plot survives a reload of the world.
///
/// The state is also the only place where the plot lives: the mob has no
/// public field for it anymore, so the code that needs the plot goes through
/// the job state ({@code mob.jobState()}) instead of touching a field of a
/// concrete mob class.
public class FarmerJobState extends EmployeeJobState {

	/// Key of the plot inside the tag of the job state.
	public static final String NBT_FARM_AREA = "FarmArea";

	/// Keys of the two corners of the plot.
	private static final String NBT_A = "A";
	private static final String NBT_B = "B";

	/// Keys of the coordinates of a corner.
	private static final String NBT_X = "X";
	private static final String NBT_Y = "Y";
	private static final String NBT_Z = "Z";

	/// The plot of this farmer: the field it works, empty when it has none.
	private Optional<PoscArea.Area2D> farmArea = Optional.empty();

	/// A fresh state with a plot already assigned.
	///
	/// This is what the FarmStick uses to hand a plot over to an employee
	/// that just became a farmer.
	public static FarmerJobState withFarmArea(PoscArea.Area2D area) {
		FarmerJobState state = new FarmerJobState();
		state.setFarmArea(area);
		return state;
	}

	/// The plot of this farmer, empty when it has none.
	public Optional<PoscArea.Area2D> farmArea() {
		return farmArea;
	}

	/// Assigns the plot of this farmer. A null area clears it.
	public void setFarmArea(@Nullable PoscArea.Area2D area) {
		this.farmArea = Optional.ofNullable(area);
	}

	/// Removes the plot of this farmer (fired, or the job changed).
	public void clearFarmArea() {
		this.farmArea = Optional.empty();
	}

	// ------------------------------------------------------------------ NBT

	/// Writes the plot (both corners and their coordinates) into the tag.
	/// A farmer without a plot writes nothing.
	@Override
	public void saveToNBT(CompoundTag tag) {
		farmArea.ifPresent(area -> {
			CompoundTag plot = new CompoundTag();
			writePos(plot, NBT_A, area.a());
			writePos(plot, NBT_B, area.b());
			tag.putCompound(NBT_FARM_AREA, plot);
		});
	}

	/// Reads the plot from the tag. A tag without a plot (or with an
	/// incomplete one) leaves the farmer without a plot.
	@Override
	public void loadFromNBT(CompoundTag tag) {
		farmArea = Optional.empty();

		if (tag == null || !tag.containsKey(NBT_FARM_AREA)) {
			return;
		}

		CompoundTag plot = tag.getCompound(NBT_FARM_AREA);
		if (plot == null) {
			return;
		}

		Optional<TilePosc> a = readPos(plot, NBT_A);
		Optional<TilePosc> b = readPos(plot, NBT_B);
		if (a.isEmpty() || b.isEmpty()) {
			return;
		}

		farmArea = Optional.of(new PoscArea.Area2D(a.get(), b.get()));
	}

	private static void writePos(CompoundTag tag, String key, TilePosc pos) {
		CompoundTag corner = new CompoundTag();
		corner.putInt(NBT_X, pos.x());
		corner.putInt(NBT_Y, pos.y());
		corner.putInt(NBT_Z, pos.z());
		tag.putCompound(key, corner);
	}

	private static Optional<TilePosc> readPos(CompoundTag tag, String key) {
		if (!tag.containsKey(key)) {
			return Optional.empty();
		}
		CompoundTag corner = tag.getCompound(key);
		if (corner == null) {
			return Optional.empty();
		}
		return Optional.of(new TilePos(
			corner.getInteger(NBT_X),
			corner.getInteger(NBT_Y),
			corner.getInteger(NBT_Z)
		));
	}
}
