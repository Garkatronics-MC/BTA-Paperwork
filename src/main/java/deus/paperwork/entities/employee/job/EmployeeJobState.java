package deus.paperwork.entities.employee.job;

import com.mojang.nbt.tags.CompoundTag;

/// The mutable state a job owns per mob: the farmer's plot, for instance.
/// One instance lives in the mob (never in the job, which is a shared
/// singleton), and it is saved/loaded with the mob's NBT.
///
/// The base state is empty: jobs that need something override the two hooks
/// and store their fields in the tag they receive.
public class EmployeeJobState {

	/// Writes this state's fields into the given tag.
	public void saveToNBT(CompoundTag tag) {}

	/// Reads this state's fields from the given tag.
	public void loadFromNBT(CompoundTag tag) {}
}
