package com.fitness.procoach.nutrition.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Testes de domínio do catálogo de alimentos (T-086): macros por 100 g aceitam
 * zero, alimento CUSTOM exige coach e aprovação, alimento global não tem coach.
 */
class FoodTest {

	private static final FoodMacros MACROS = FoodMacros.of(119, 21.5, 0, 3);

	@Test
	void criaCustomVinculadoAoCoachEAprovado() {
		UUID coachId = UUID.randomUUID();
		Food food = Food.createCustom(coachId, "Whey do coach", "Suplementos", MACROS, 100,
				"1 scoop", 30.0, FoodState.AS_SERVED);

		assertTrue(food.isCustom());
		assertEquals(FoodSource.CUSTOM, food.source());
		assertEquals(coachId, food.coachId());
		assertTrue(food.coachApproved());
		assertFalse(food.isDeleted());
		assertNull(food.tacoId());
	}

	@Test
	void customExigeCoach() {
		assertThrows(NullPointerException.class, () -> Food.createCustom(null, "Whey", null, MACROS, 100,
				null, null, FoodState.AS_SERVED));
	}

	@Test
	void macrosAceitamZeroMasNaoNegativo() {
		assertEquals(0.0, MACROS.carbohydrates());
		assertThrows(IllegalArgumentException.class, () -> FoodMacros.of(1, -1, 0, 0));
	}

	@Test
	void porcaoComestivelDeveEstarEntreZeroECem() {
		assertThrows(IllegalArgumentException.class, () -> Food.createCustom(UUID.randomUUID(), "Whey",
				null, MACROS, 0, null, null, FoodState.AS_SERVED));
		assertThrows(IllegalArgumentException.class, () -> Food.createCustom(UUID.randomUUID(), "Whey",
				null, MACROS, 101, null, null, FoodState.AS_SERVED));
	}

	@Test
	void alimentoGlobalNaoPodeTerCoach() {
		assertThrows(IllegalArgumentException.class, () -> Food.reconstitute(FoodId.random(),
				"taco:arroz", "Arroz", "Cereais", MACROS, 100, null, null, FoodSource.TACO,
				FoodState.RAW, UUID.randomUUID(), false, false, java.time.Instant.now(),
				java.time.Instant.now()));
	}

	@Test
	void alimentoGlobalEhReconstituidoSemCoach() {
		Food food = Food.reconstitute(FoodId.random(), "taco:arroz", "Arroz", "Cereais", MACROS,
				100, "1 colher de sopa", 15.0, FoodSource.TACO, FoodState.RAW, null, false,
				false, java.time.Instant.now(), java.time.Instant.now());

		assertFalse(food.isCustom());
		assertNull(food.coachId());
		assertEquals("1 colher de sopa", food.householdMeasure());
		assertEquals(FoodState.RAW, food.stateDefault());
	}
}
