package com.fitness.procoach.training.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitness.procoach.training.domain.model.ExercisePrescription;
import com.fitness.procoach.training.domain.model.IntensityType;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PrescriptionChangesTest {

	private static final UUID EXERCISE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

	private static ExercisePrescription prescription(UUID exerciseId, int sets, int reps, String load, String notes) {
		return new ExercisePrescription(UUID.fromString("22222222-2222-2222-2222-222222222222"), exerciseId, "Supino",
				false, 0, sets, reps, new BigDecimal(load), IntensityType.RPE, new BigDecimal("8.0"), 90, notes);
	}

	@Test
	void semMudancaNaoHaDiferencas() {
		ExercisePrescription a = prescription(EXERCISE_ID, 4, 10, "60.0", null);
		ExercisePrescription b = prescription(EXERCISE_ID, 4, 10, "60.00", null);

		assertThat(PrescriptionChanges.between(a, b)).isEmpty();
	}

	@Test
	void descreveSeriesERepeticoesComAntesEDepois() {
		var changes = PrescriptionChanges.between(prescription(EXERCISE_ID, 4, 10, "60.0", null),
				prescription(EXERCISE_ID, 5, 12, "60.0", null));

		assertThat(changes).extracting(PrescriptionChanges.Change::field).containsExactly("sets", "repetitions");
		assertThat(PrescriptionChanges.describe(changes)).isEqualTo("séries 4 → 5, repetições 10 → 12");
	}

	@Test
	void cargaUsaNumeroSemZerosASobrar() {
		var changes = PrescriptionChanges.between(prescription(EXERCISE_ID, 4, 10, "60.0", null),
				prescription(EXERCISE_ID, 4, 10, "62.50", null));

		assertThat(PrescriptionChanges.describe(changes)).isEqualTo("carga (kg) 60 → 62.5");
	}

	@Test
	void cargaInteiraNaoViraNotacaoCientifica() {
		var changes = PrescriptionChanges.between(prescription(EXERCISE_ID, 4, 10, "100", null),
				prescription(EXERCISE_ID, 4, 10, "120", null));

		assertThat(PrescriptionChanges.describe(changes)).isEqualTo("carga (kg) 100 → 120");
	}

	@Test
	void trocaDeVersaoDoExercicioNaoMostraIds() {
		UUID newer = UUID.fromString("33333333-3333-3333-3333-333333333333");
		var changes = PrescriptionChanges.between(prescription(EXERCISE_ID, 4, 10, "60.0", null),
				prescription(newer, 4, 10, "60.0", null));

		assertThat(PrescriptionChanges.describe(changes)).isEqualTo("exercício trocado pela versão mais recente");
		assertThat(PrescriptionChanges.describe(changes)).doesNotContain(newer.toString());
	}

	@Test
	void observacoesAlteradasNaoVazamOConteudo() {
		var changes = PrescriptionChanges.between(prescription(EXERCISE_ID, 4, 10, "60.0", null),
				prescription(EXERCISE_ID, 4, 10, "60.0", "texto livre sensível"));

		assertThat(PrescriptionChanges.describe(changes)).isEqualTo("observações atualizadas");
		String data = PrescriptionChanges.auditData(UUID.randomUUID(), changes);
		assertThat(data).contains("\"changed\":\"notes\"").doesNotContain("texto livre");
	}

	@Test
	void auditoriaGuardaAntesEDepoisDeCadaCampo() {
		UUID prescriptionId = UUID.fromString("22222222-2222-2222-2222-222222222222");
		var changes = PrescriptionChanges.between(prescription(EXERCISE_ID, 4, 10, "60.0", null),
				prescription(EXERCISE_ID, 5, 10, "62.5", null));

		String data = PrescriptionChanges.auditData(prescriptionId, changes);

		assertThat(data).contains("\"prescriptionId\":\"" + prescriptionId + "\"")
				.contains("\"changed\":\"sets,load\"")
				.contains("\"setsBefore\":\"4\"", "\"setsAfter\":\"5\"")
				.contains("\"loadBefore\":\"60\"", "\"loadAfter\":\"62.5\"");
	}

	@Test
	void todosOsCamposAlteradosAparecem() {
		ExercisePrescription before = prescription(EXERCISE_ID, 4, 10, "60.0", null);
		ExercisePrescription after = new ExercisePrescription(before.getId(), EXERCISE_ID, "Supino inclinado", false, 0,
				5, 12, new BigDecimal("65"), IntensityType.RIR, new BigDecimal("2"), 120, null);

		List<PrescriptionChanges.Change> changes = PrescriptionChanges.between(before, after);

		assertThat(changes).extracting(PrescriptionChanges.Change::field).containsExactly("exerciseName", "sets",
				"repetitions", "load", "intensityType", "intensityValue", "restSeconds");
	}
}
