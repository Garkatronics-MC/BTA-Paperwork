package deus.paperwork.entity.employee.chef;

import com.mojang.nbt.tags.CompoundTag;
import deus.paperwork.entity.employee.job.EmployeeJobState;
import deus.paperwork.core.util.PoscArea;
import net.minecraft.core.world.pos.TilePos;
import net.minecraft.core.world.pos.TilePosc;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/// The mutable state of the chef job: the workplace area and the last
/// source/destination chests used by the cook task.
///
/// One instance lives in every chef (the job itself is a shared singleton),
/// and it is saved with the mob: the area and the chest choices survive a
/// reload of the world.
public class ChefJobState extends EmployeeJobState {

	public static final String NBT_WORK_AREA = "WorkArea";
	public static final String NBT_SOURCE_CHEST = "SourceChest";
	public static final String NBT_DEST_CHEST = "DestChest";

	private static final String NBT_A = "A";
	private static final String NBT_B = "B";
	private static final String NBT_X = "X";
	private static final String NBT_Y = "Y";
	private static final String NBT_Z = "Z";

	/// The workplace of this chef: where it looks for furnace, workbench
	/// and chests. Empty when it has none.
	private Optional<PoscArea.Area2D> workArea = Optional.empty();

	/// The last chest the chef took raw ingredients from.
	private Optional<TilePosc> sourceChest = Optional.empty();

	/// The last chest the chef deposited cooked food into (never the same as
	/// sourceChest).
	private Optional<TilePosc> destChest = Optional.empty();

	/// A fresh state with a workplace already assigned.
	public static ChefJobState withWorkArea(PoscArea.Area2D area) {
		ChefJobState state = new ChefJobState();
		state.setWorkArea(area);
		return state;
	}

	public Optional<PoscArea.Area2D> workArea() {
		return workArea;
	}

	public void setWorkArea(@Nullable PoscArea.Area2D area) {
		this.workArea = Optional.ofNullable(area);
	}

	public void clearWorkArea() {
		this.workArea = Optional.empty();
	}

	public Optional<TilePosc> sourceChest() {
		return sourceChest;
	}

	public void setSourceChest(@Nullable TilePosc pos) {
		this.sourceChest = Optional.ofNullable(pos);
	}

	public Optional<TilePosc> destChest() {
		return destChest;
	}

	public void setDestChest(@Nullable TilePosc pos) {
		this.destChest = Optional.ofNullable(pos);
	}

	public void clearDestChest() {
		this.destChest = Optional.empty();
	}

	// ------------------------------------------------------------------ NBT

	@Override
	public void saveToNBT(CompoundTag tag) {
		workArea.ifPresent(area -> {
			CompoundTag areaTag = new CompoundTag();
			writePos(areaTag, NBT_A, area.a());
			writePos(areaTag, NBT_B, area.b());
			tag.putCompound(NBT_WORK_AREA, areaTag);
		});
		sourceChest.ifPresent(pos -> {
			CompoundTag chestTag = new CompoundTag();
			writePos(chestTag, NBT_X, pos);
			tag.putCompound(NBT_SOURCE_CHEST, chestTag);
		});
		destChest.ifPresent(pos -> {
			CompoundTag chestTag = new CompoundTag();
			writePos(chestTag, NBT_X, pos);
			tag.putCompound(NBT_DEST_CHEST, chestTag);
		});
	}

	@Override
	public void loadFromNBT(CompoundTag tag) {
		workArea = Optional.empty();
		sourceChest = Optional.empty();
		destChest = Optional.empty();

		if (tag == null) {
			return;
		}

		if (tag.containsKey(NBT_WORK_AREA)) {
			CompoundTag areaTag = tag.getCompound(NBT_WORK_AREA);
			if (areaTag != null) {
				Optional<TilePosc> a = readPos(areaTag, NBT_A);
				Optional<TilePosc> b = readPos(areaTag, NBT_B);
				if (a.isPresent() && b.isPresent()) {
					workArea = Optional.of(new PoscArea.Area2D(a.get(), b.get()));
				}
			}
		}

		if (tag.containsKey(NBT_SOURCE_CHEST)) {
			CompoundTag chestTag = tag.getCompound(NBT_SOURCE_CHEST);
			if (chestTag != null) {
				readPos(chestTag, NBT_X).ifPresent(this::setSourceChest);
			}
		}

		if (tag.containsKey(NBT_DEST_CHEST)) {
			CompoundTag chestTag = tag.getCompound(NBT_DEST_CHEST);
			if (chestTag != null) {
				readPos(chestTag, NBT_X).ifPresent(this::setDestChest);
			}
		}
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
