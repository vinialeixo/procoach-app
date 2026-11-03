package com.fitness.procoach.training.application;

import com.fitness.procoach.audit.application.AuditData;
import com.fitness.procoach.training.domain.model.ExercisePrescription;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Diferença entre duas versões de uma prescrição: alimenta o histórico (antes/depois) e o texto
 * do aviso ao atleta. Observações livres (notes) entram só como "alteradas", sem o conteúdo.
 */
final class PrescriptionChanges {

	/** Um campo alterado. {@code showValues=false} omite antes/depois (texto livre ou ids). */
	record Change(String field, String label, Object before, Object after, boolean showValues) {
	}

	private PrescriptionChanges() {
	}

	static List<Change> between(ExercisePrescription before, ExercisePrescription after) {
		List<Change> changes = new ArrayList<>();
		if (!Objects.equals(before.getExerciseName(), after.getExerciseName())) {
			changes.add(new Change("exerciseName", "nome do exercício", before.getExerciseName(),
					after.getExerciseName(), true));
		}
		if (!Objects.equals(before.getExerciseId(), after.getExerciseId())) {
			changes.add(new Change("exerciseId", "versão do exercício", before.getExerciseId(),
					after.getExerciseId(), false));
		}
		if (before.getSets() != after.getSets()) {
			changes.add(new Change("sets", "séries", before.getSets(), after.getSets(), true));
		}
		if (before.getRepetitions() != after.getRepetitions()) {
			changes.add(new Change("repetitions", "repetições", before.getRepetitions(), after.getRepetitions(), true));
		}
		if (before.getLoad().compareTo(after.getLoad()) != 0) {
			changes.add(new Change("load", "carga (kg)", before.getLoad(), after.getLoad(), true));
		}
		if (before.getIntensityType() != after.getIntensityType()) {
			changes.add(new Change("intensityType", "tipo de intensidade", before.getIntensityType(),
					after.getIntensityType(), true));
		}
		if (before.getIntensityValue().compareTo(after.getIntensityValue()) != 0) {
			changes.add(new Change("intensityValue", "intensidade", before.getIntensityValue(),
					after.getIntensityValue(), true));
		}
		if (before.getRestSeconds() != after.getRestSeconds()) {
			changes.add(new Change("restSeconds", "descanso (s)", before.getRestSeconds(), after.getRestSeconds(),
					true));
		}
		if (!Objects.equals(before.getNotes(), after.getNotes())) {
			changes.add(new Change("notes", "observações", null, null, false));
		}
		return changes;
	}

	/** Payload do histórico: campos alterados e, para cada um, o antes e o depois. */
	static String auditData(UUID prescriptionId, List<Change> changes) {
		List<Object> pairs = new ArrayList<>();
		pairs.add("prescriptionId");
		pairs.add(prescriptionId.toString());
		pairs.add("changed");
		pairs.add(changes.stream().map(Change::field).collect(Collectors.joining(",")));
		for (Change change : changes) {
			if (change.field().equals("notes")) {
				continue;
			}
			pairs.add(change.field() + "Before");
			pairs.add(format(change.before()));
			pairs.add(change.field() + "After");
			pairs.add(format(change.after()));
		}
		return AuditData.of(pairs.toArray());
	}

	/** Texto para o atleta, ex.: "séries 4 → 5, repetições 10 → 12". */
	static String describe(List<Change> changes) {
		return changes.stream().map(change -> {
			if (change.showValues()) {
				return change.label() + " " + format(change.before()) + " → " + format(change.after());
			}
			return change.field().equals("exerciseId") ? "exercício trocado pela versão mais recente"
					: "observações atualizadas";
		}).collect(Collectors.joining(", "));
	}

	private static String format(Object value) {
		if (value == null) {
			return "—";
		}
		if (value instanceof BigDecimal decimal) {
			return decimal.stripTrailingZeros().toPlainString();
		}
		return value.toString();
	}
}
