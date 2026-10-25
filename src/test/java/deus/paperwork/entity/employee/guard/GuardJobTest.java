package deus.paperwork.entity.employee.guard;

import deus.paperwork.entity.employee.job.EmployeeJobState;
import deus.paperwork.entity.employee.job.EmployeeJobs;
import deus.paperwork.entity.employee.model.EmployeeStations;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// El oficio guard sin un mob: el id, el aspecto, las estaciones y el estado.
///
/// El combate real necesita un World con entidades vivas, así que aquí se
/// protegen las partes puras que no deben derivar.
class GuardJobTest {

	/// El oficio se usa directamente (no por el registro): el registro lo llena
	/// el inicializador del mod, que no corre en los tests.
	private static final GuardJob JOB = new GuardJob();

	@BeforeAll
	static void initItems() {
		Items.init();
	}

	@Test
	@DisplayName("el oficio es el guardia")
	void idIsGuard() {
		assertEquals("guard", JOB.id());
		assertEquals(EmployeeJobs.GUARD_ID, JOB.id());
	}

	@Test
	@DisplayName("el guardia no tiene accesorio de modelo todavía")
	void theGuardHasNoAccessory() {
		assertTrue(JOB.accessories().isEmpty());
	}

	@Test
	@DisplayName("el esqueleto del guardia no tiene huesos extra")
	void modelSpecIsEmpty() {
		assertTrue(JOB.modelSpec(null).bones().isEmpty());
	}

	@Test
	@DisplayName("el guardia usa las estaciones base")
	void stationsAreTheBaseOnes() {
		EmployeeStations base = EmployeeStations.none();

		EmployeeStations result = JOB.stations(null, base);

		assertSame(base, result);
		assertTrue(result.workPlace().get().isEmpty());
	}

	@Test
	@DisplayName("un guardia sin zona de trabajo no se queja")
	void thereIsNoMissingStationAlert() {
		assertTrue(JOB.missingStationAlert(null).isEmpty());
	}

	@Test
	@DisplayName("el guardia no necesita nada concreto en su zona de trabajo")
	void thereIsNoMissingZoneItem() {
		assertNull(JOB.missingZoneItems(null, null));
	}

	@Test
	@DisplayName("sin mob no hay trabajo que hacer")
	void withoutMobThereIsNoWork() {
		assertFalse(JOB.hasWorkToDo(null));
	}

	@Test
	@DisplayName("limpiar las estaciones de un guardia no rompe")
	void clearingStationsDoesNothing() {
		JOB.clearStations(null);
		JOB.recharge(null, null);
		JOB.work(null);
	}

	@Test
	@DisplayName("el estado de un guardia es un estado de empleado nuevo y vacío")
	void theStateIsFreshAndEmpty() {
		EmployeeJobState state = JOB.createState();

		assertNotNull(state);
		assertTrue(state instanceof GuardJobState);
	}

	@Test
	@DisplayName("un objetivo null nunca es hostil")
	void aNullTargetIsNotHostile() {
		assertFalse(GuardTargeting.isHostile(null));
	}

	// ------------------------------------------------------------------ armas

	@Test
	@DisplayName("un arco se reconoce como arco")
	void recognizesBow() {
		assertTrue(GuardJob.isBow(Items.TOOL_BOW));
		assertFalse(GuardJob.isBow(Items.STICK));
	}

	@Test
	@DisplayName("una espada con daño contra entidades se reconoce como espada")
	void recognizesSword() {
		ItemStack sword = new ItemStack(Items.TOOL_SWORD_IRON, 1);
		assertTrue(GuardJob.isSword(sword));
	}

	@Test
	@DisplayName("un arco no se confunde con espada")
	void bowIsNotSword() {
		ItemStack bow = new ItemStack(Items.TOOL_BOW, 1);
		assertFalse(GuardJob.isSword(bow));
	}

	@Test
	@DisplayName("null no es espada")
	void nullIsNotSword() {
		assertFalse(GuardJob.isSword(null));
	}
}
