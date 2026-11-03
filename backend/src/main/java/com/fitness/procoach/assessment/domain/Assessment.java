package com.fitness.procoach.assessment.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Agregado raiz do bounded context Assessment (Avaliação Física).
 *
 * <p>Regras de domínio aplicadas:
 * <ul>
 *   <li>A data da avaliação é obrigatória (US-007).</li>
 *   <li>O agregado é imutável: cada nova avaliação cria um novo registro, nunca
 *       sobrescreve o anterior (US-008 / regra inegociável 3).</li>
 *   <li>Referências a outros agregados são sempre por UUID (coachId, athleteId).</li>
 *   <li>Domínio framework-agnostic: nenhuma anotação Spring/JPA.</li>
 * </ul>
 */
public final class Assessment {

	private final AssessmentId id;
	private final UUID athleteId;
	private final UUID coachId;
	private final LocalDate assessmentDate;
	private final BodyMeasurements measurements;
	private final Instant createdAt;
	private final Instant updatedAt;
	private final long version;

	private Assessment(AssessmentId id, UUID athleteId, UUID coachId, LocalDate assessmentDate,
			BodyMeasurements measurements, Instant createdAt, Instant updatedAt, long version) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.athleteId = Objects.requireNonNull(athleteId, "athleteId must not be null");
		this.coachId = Objects.requireNonNull(coachId, "coachId must not be null");
		this.assessmentDate = Objects.requireNonNull(assessmentDate, "assessmentDate must not be null");
		this.measurements = Objects.requireNonNull(measurements, "measurements must not be null");
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		this.version = version;
	}

	/**
	 * Cria uma nova avaliação. A data é obrigatória — lança exceção se ausente.
	 */
	public static Assessment create(UUID athleteId, UUID coachId, LocalDate assessmentDate,
			BodyMeasurements measurements) {
		Objects.requireNonNull(assessmentDate, "assessmentDate must not be null");
		Instant now = Instant.now();
		return new Assessment(AssessmentId.generate(), athleteId, coachId, assessmentDate, measurements, now, now, 1L);
	}

	/**
	 * Reconstitui o agregado a partir do estado persistido (uso exclusivo da
	 * camada de infraestrutura).
	 */
	public static Assessment reconstitute(AssessmentId id, UUID athleteId, UUID coachId,
			LocalDate assessmentDate, BodyMeasurements measurements, Instant createdAt,
			Instant updatedAt, long version) {
		return new Assessment(id, athleteId, coachId, assessmentDate, measurements, createdAt, updatedAt, version);
	}

	public AssessmentId id() {
		return id;
	}

	public UUID athleteId() {
		return athleteId;
	}

	public UUID coachId() {
		return coachId;
	}

	public LocalDate assessmentDate() {
		return assessmentDate;
	}

	public BodyMeasurements measurements() {
		return measurements;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public Instant updatedAt() {
		return updatedAt;
	}

	public long version() {
		return version;
	}
}
