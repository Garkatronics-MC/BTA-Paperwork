package deus.paperwork.entities.employee.job;

import com.mojang.nbt.tags.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmployeeJobStateTest {

	@Test
	void stateWithDataSurvivesTheNbtRoundTrip() {
		IntState written = new IntState();
		written.value = 42;

		CompoundTag tag = new CompoundTag();
		written.saveToNBT(tag);

		IntState read = new IntState();
		read.loadFromNBT(tag);

		assertEquals(42, read.value);
	}

	@Test
	void defaultStateSurvivesTheNbtRoundTrip() {
		EmployeeJobState written = new EmployeeJobState();

		CompoundTag tag = new CompoundTag();
		written.saveToNBT(tag);

		EmployeeJobState read = new EmployeeJobState();
		read.loadFromNBT(tag);

		assertTrue(tag.getValues().isEmpty());
	}

	@Test
	void loadingAnEmptyTagDoesNotBreak() {
		IntState read = new IntState();
		read.loadFromNBT(new CompoundTag());
		assertEquals(0, read.value);
	}

	/// A job state that keeps a single int, the smallest possible payload.
	private static final class IntState extends EmployeeJobState {

		private int value = 0;

		@Override
		public void saveToNBT(CompoundTag tag) {
			tag.putInt("value", value);
		}

		@Override
		public void loadFromNBT(CompoundTag tag) {
			value = tag.getInteger("value");
		}
	}
}
