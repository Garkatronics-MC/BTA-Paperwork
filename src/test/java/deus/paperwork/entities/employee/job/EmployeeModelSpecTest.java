package deus.paperwork.entities.employee.job;

import deus.paperwork.entities.employee.job.EmployeeModelSpec.EmployeeBone;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmployeeModelSpecTest {

	@Test
	void emptyHasNoBones() {
		assertTrue(EmployeeModelSpec.empty().bones().isEmpty());
	}

	@Test
	void hiddenBoneIsInvisibleAndUnposed() {
		EmployeeBone bone = EmployeeBone.hidden("hat");
		assertEquals("hat", bone.name());
		assertFalse(bone.visible());
		assertNull(bone.pitch());
		assertNull(bone.yaw());
		assertNull(bone.roll());
	}

	@Test
	void shownBoneIsVisibleAndUnposed() {
		EmployeeBone bone = EmployeeBone.shown("wheat1");
		assertEquals("wheat1", bone.name());
		assertTrue(bone.visible());
		assertNull(bone.pitch());
		assertNull(bone.yaw());
		assertNull(bone.roll());
	}

	@Test
	void posedBoneKeepsItsRotation() {
		EmployeeBone bone = EmployeeBone.posed("arm_r", 1.0, -2.0, 0.5);
		assertEquals("arm_r", bone.name());
		assertTrue(bone.visible());
		assertEquals(1.0, bone.pitch());
		assertEquals(-2.0, bone.yaw());
		assertEquals(0.5, bone.roll());
	}

	@Test
	void boneListIsImmutable() {
		EmployeeModelSpec spec = new EmployeeModelSpec(List.of(EmployeeBone.shown("hat")));
		assertThrows(UnsupportedOperationException.class, () -> spec.bones().add(EmployeeBone.hidden("hat")));
	}

	@Test
	void boneListIsADefensiveCopy() {
		List<EmployeeBone> source = new ArrayList<>();
		source.add(EmployeeBone.shown("hat"));

		EmployeeModelSpec spec = new EmployeeModelSpec(source);
		source.add(EmployeeBone.hidden("wheat1"));

		assertEquals(1, spec.bones().size());
		assertEquals("hat", spec.bones().get(0).name());
	}
}
