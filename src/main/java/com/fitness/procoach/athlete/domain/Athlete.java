package com.fitness.procoach.athlete.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root do bounded context athlete (T-048).
 *
 * <p>O {@code id} é único e imutável (US-002): gerado automaticamente pelo
 * sistema e nunca alterado ou reutilizado após a criação. Referências a outros
 * agregados (coach) são sempre por UUID, nunca por objeto aninhado.</p>
 *
 * <p>O domínio é framework-agnostic: nenhuma anotação Spring/JPA aqui.</p>
 */
public final class Athlete {

	private final AthleteId id;
	private final UUID coachId;
	private final Name name;
	private final Sex sex;
	private final LocalDate birthDate;
	private final Height height;
	private final Weight weight;
	private final Goal goal;
	private final AthleteStatus status;
	private final Instant createdAt;
	private Instant updatedAt;
	private boolean deleted;
	private Instant deletedAt;

	private Athlete(AthleteId id, UUID coachId, Name name, Sex sex, LocalDate birthDate,
			Height height, Weight weight, Goal goal, AthleteStatus status,
			Instant createdAt, Instant updatedAt, boolean deleted, Instant deletedAt) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.coachId = Objects.requireNonNull(coachId, "coachId must not be null");
		this.name = Objects.requireNonNull(name, "name must not be null");
		this.sex = Objects.requireNonNull(sex, "sex must not be null");
		this.birthDate = Objects.requireNonNull(birthDate, "birthDate must not be null");
		if (birthDate.isAfter(LocalDate.now())) {
			throw new IllegalArgumentException("birthDate must not be in the future");
		}
		this.height = Objects.requireNonNull(height, "height must not be null");
		this.weight = Objects.requireNonNull(weight, "weight must not be null");
		this.goal = Objects.requireNonNull(goal, "goal must not be null");
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		this.deleted = deleted;
		this.deletedAt = deletedAt;
	}

	/**
	 * Cria um novo atleta (US-001). O ID é gerado pelo chamador via
	 * {@link AthleteId#generate()}; o status inicial é {@code ACTIVE}.
	 */
	public static Athlete register(AthleteId id, UUID coachId, Name name, Sex sex, LocalDate birthDate,
			Height height, Weight weight, Goal goal) {
		Instant now = Instant.now();
		return new Athlete(id, coachId, name, sex, birthDate, height, weight, goal,
				AthleteStatus.ACTIVE, now, now, false, null);
	}

	/**
	 * Reconstrói um atleta a partir do estado persistido (uso do adapter de
	 * infraestrutura). Não gera novos timestamps nem altera o status.
	 */
	public static Athlete reconstitute(AthleteId id, UUID coachId, Name name, Sex sex, LocalDate birthDate,
			Height height, Weight weight, Goal goal, AthleteStatus status,
			Instant createdAt, Instant updatedAt, boolean deleted, Instant deletedAt) {
		return new Athlete(id, coachId, name, sex, birthDate, height, weight, goal,
				status, createdAt, updatedAt, deleted, deletedAt);
	}

	/**
	 * Exclusão lógica do atleta (US-002/UML 4.1.2): o registro nunca é removido
	 * fisicamente — é apenas marcado como excluído, preservando o histórico.
	 */
	public void softDelete() {
		if (this.deleted) {
			throw new IllegalStateException("Athlete has been soft-deleted: " + this.id);
		}
		this.deleted = true;
		this.deletedAt = Instant.now();
		this.updatedAt = Instant.now();
	}

	public AthleteId id() {
		return id;
	}

	public UUID coachId() {
		return coachId;
	}

	public Name name() {
		return name;
	}

	public Sex sex() {
		return sex;
	}

	public LocalDate birthDate() {
		return birthDate;
	}

	public Height height() {
		return height;
	}

	public Weight weight() {
		return weight;
	}

	public Goal goal() {
		return goal;
	}

	public AthleteStatus status() {
		return status;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public Instant updatedAt() {
		return updatedAt;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public Instant deletedAt() {
		return deletedAt;
	}
}
