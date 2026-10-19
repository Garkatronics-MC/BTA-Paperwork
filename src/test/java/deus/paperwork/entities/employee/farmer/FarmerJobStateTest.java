package deus.paperwork.entities.employee.farmer;

import com.mojang.nbt.tags.CompoundTag;
import deus.paperwork.util.PoscArea;
import net.minecraft.core.world.pos.TilePos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The NBT round trip of the plot of a farmer.
class FarmerJobStateTest {

	@Test
	@DisplayName("the plot survives the NBT round trip")
	void thePlotSurvivesTheNbtRoundTrip() {
		PoscArea.Area2D plot = new PoscArea.Area2D(new TilePos(3, 64, -7), new TilePos(11, 64, 5));
		FarmerJobState written = FarmerJobState.withFarmArea(plot);

		CompoundTag tag = new CompoundTag();
		written.saveToNBT(tag);

		FarmerJobState read = new FarmerJobState();
		read.loadFromNBT(tag);

		PoscArea.Area2D area = read.farmArea().orElseThrow(() -> new AssertionError("the plot is gone"));
		assertEquals(3, area.a().x());
		assertEquals(64, area.a().y());
		assertEquals(-7, area.a().z());
		assertEquals(11, area.b().x());
		assertEquals(64, area.b().y());
		assertEquals(5, area.b().z());
	}

	@Test
	@DisplayName("a farmer without a plot stays without a plot")
	void withoutAPlotTheRoundTripKeepsItEmpty() {
		FarmerJobState written = new FarmerJobState();

		CompoundTag tag = new CompoundTag();
		written.saveToNBT(tag);

		FarmerJobState read = new FarmerJobState();
		read.loadFromNBT(tag);

		assertTrue(read.farmArea().isEmpty());
		assertTrue(tag.getValues().isEmpty(), "a farmer without a plot writes nothing");
	}

	@Test
	@DisplayName("a plot assigned after the round trip is not lost either")
	void aPlotAssignedAfterTheRoundTripIsKept() {
		FarmerJobState written = new FarmerJobState();

		CompoundTag tag = new CompoundTag();
		written.saveToNBT(tag);

		FarmerJobState read = new FarmerJobState();
		read.loadFromNBT(tag);
		read.setFarmArea(new PoscArea.Area2D(new TilePos(1, 64, 1), new TilePos(2, 64, 2)));

		assertTrue(read.farmArea().isPresent());
	}

	@Test
	@DisplayName("the plot can be taken away")
	void thePlotCanBeCleared() {
		FarmerJobState state = FarmerJobState.withFarmArea(
			new PoscArea.Area2D(new TilePos(0, 64, 0), new TilePos(1, 64, 1))
		);
		assertTrue(state.farmArea().isPresent());

		state.clearFarmArea();

		assertTrue(state.farmArea().isEmpty());
	}

	@Test
	@DisplayName("an empty tag does not break the state")
	void anEmptyTagDoesNotBreakTheState() {
		FarmerJobState state = new FarmerJobState();
		state.loadFromNBT(new CompoundTag());
		assertTrue(state.farmArea().isEmpty());
	}
}
