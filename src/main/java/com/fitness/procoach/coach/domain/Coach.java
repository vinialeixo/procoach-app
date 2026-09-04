package com.fitness.procoach.coach.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root do bounded context coach (T-058), conforme UML seção 4/12.1:
 * {@code User "1" --> "1" Coach : is} — o Coach referencia o User por id
 * ({@code userId}) e é o dono da carteira de atletas.
 *
 * <p>A carteira em si (Coach "1" --> "0..*" Athlete) não é estado duplicado
 * deste agregado: a referência vive em {@code Athlete.coachId}. Os métodos
 * {@link #addAthlete(UUID)}/{@link #removeAthlete(UUID)} guardam os invariantes
 * da operação (somente um coach ACTIVE gerencia a própria carteira) e a
 * orquestração fica no application service.</p>
 *
 * <p>O domínio é framework-agnostic: nenhuma anotação Spring/JPA aqui.</p>
 */
public final class Coach {

	private final CoachId id;
	private final UUID userId;
	private final String name;
	private CoachStatus status;
	private final Instant createdAt;
	private Instant updatedAt;

	private Coach(CoachId id, UUID userId, String name, CoachStatus status,
			Instant createdAt, Instant updatedAt) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.userId = Objects.requireNonNull(userId, "userId must not be null");
		this.name = requireName(name);
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
	}

	/**
	 * Registra um novo coach vinculado ao usuário autenticável (User.id). O ID é
	 * gerado pelo chamador via {@link CoachId#generate()}; o status inicial é
	 * {@code ACTIVE}.
	 */
	public static Coach register(CoachId id, UUID userId, String name) {
		Instant now = Instant.now();
		return new Coach(id, userId, name, CoachStatus.ACTIVE, now, now);
	}

	/**
	 * Reconstrói um coach a partir do estado persistido (uso do adapter de
	 * infraestrutura). Não gera novos timestamps nem altera o status.
	 */
	public static Coach reconstitute(CoachId id, UUID userId, String name, CoachStatus status,
			Instant createdAt, Instant updatedAt) {
		return new Coach(id, userId, name, status, createdAt, updatedAt);
	}

	/**
	 * Registra um atleta na carteira deste coach (UML 12.2). Exige coach ACTIVE.
	 */
	public void addAthlete(UUID athleteId) {
		Objects.requireNonNull(athleteId, "athleteId must not be null");
		ensureActive("add athletes to the roster");
		touch();
	}

	/**
	 * Remove um atleta da carteira deste coach. Exige coach ACTIVE.
	 */
	public void removeAthlete(UUID athleteId) {
		Objects.requireNonNull(athleteId, "athleteId must not be null");
		ensureActive("remove athletes from the roster");
		touch();
	}

	/** ACTIVE -&gt; SUSPENDED (reversível via {@link #reactivate()}). */
	public void suspend() {
		if (this.status != CoachStatus.ACTIVE) {
			throw new IllegalStateException("Only ACTIVE coaches can be suspended, current: " + this.status);
		}
		this.status = CoachStatus.SUSPENDED;
		touch();
	}

	/** SUSPENDED -&gt; ACTIVE. */
	public void reactivate() {
		if (this.status != CoachStatus.SUSPENDED) {
			throw new IllegalStateException("Only SUSPENDED coaches can be reactivated, current: " + this.status);
		}
		this.status = CoachStatus.ACTIVE;
		touch();
	}

	/** ACTIVE/SUSPENDED -&gt; CANCELLED (terminal). */
	public void cancel() {
		if (this.status == CoachStatus.CANCELLED) {
			throw new IllegalStateException("Coach is already cancelled");
		}
		this.status = CoachStatus.CANCELLED;
		touch();
	}

	private void ensureActive(String operation) {
		if (this.status != CoachStatus.ACTIVE) {
			throw new IllegalStateException(
					"Coach " + this.id + " cannot " + operation + " while " + this.status);
		}
	}

	private static String requireName(String name) {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("name must not be blank");
		}
		String trimmed = name.trim();
		if (trimmed.length() < 2 || trimmed.length() > 120) {
			throw new IllegalArgumentException("name must have between 2 and 120 characters");
		}
		return trimmed;
	}

	private void touch() {
		this.updatedAt = Instant.now();
	}

	public CoachId id() {
		return id;
	}

	public UUID userId() {
		return userId;
	}

	public String name() {
		return name;
	}

	public CoachStatus status() {
		return status;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public Instant updatedAt() {
		return updatedAt;
	}
}
