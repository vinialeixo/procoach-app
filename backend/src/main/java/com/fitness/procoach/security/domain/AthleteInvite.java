package com.fitness.procoach.security.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Convite de uso único para o atleta criar a própria credencial. O token em si
 * nunca é guardado — só o hash. Vale até {@code expiresAt}, uma única vez, e é
 * revogado quando o coach gera um convite novo para o mesmo atleta.
 */
public final class AthleteInvite {

	private final UUID id;
	private final UUID athleteId;
	private final UUID coachId;
	private final String email;
	private final String tokenHash;
	private final Instant expiresAt;
	private Instant usedAt;
	private Instant revokedAt;
	private final Instant createdAt;

	private AthleteInvite(UUID id, UUID athleteId, UUID coachId, String email, String tokenHash, Instant expiresAt,
			Instant usedAt, Instant revokedAt, Instant createdAt) {
		this.id = Objects.requireNonNull(id, "id");
		this.athleteId = Objects.requireNonNull(athleteId, "athleteId");
		this.coachId = Objects.requireNonNull(coachId, "coachId");
		this.email = Objects.requireNonNull(email, "email");
		this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash");
		this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
		this.usedAt = usedAt;
		this.revokedAt = revokedAt;
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
	}

	public static AthleteInvite create(UUID athleteId, UUID coachId, String email, String tokenHash, Instant now,
			Instant expiresAt) {
		if (email.isBlank()) {
			throw new IllegalArgumentException("email must not be blank");
		}
		if (!expiresAt.isAfter(now)) {
			throw new IllegalArgumentException("expiresAt must be in the future");
		}
		return new AthleteInvite(UUID.randomUUID(), athleteId, coachId, email, tokenHash, expiresAt, null, null, now);
	}

	public static AthleteInvite reconstitute(UUID id, UUID athleteId, UUID coachId, String email, String tokenHash,
			Instant expiresAt, Instant usedAt, Instant revokedAt, Instant createdAt) {
		return new AthleteInvite(id, athleteId, coachId, email, tokenHash, expiresAt, usedAt, revokedAt, createdAt);
	}

	/** Pendente: não usado, não revogado e ainda dentro do prazo. */
	public boolean isUsable(Instant now) {
		return usedAt == null && revokedAt == null && now.isBefore(expiresAt);
	}

	public void markUsed(Instant now) {
		if (!isUsable(now)) {
			throw new IllegalStateException("Invite is not usable");
		}
		this.usedAt = now;
	}

	/** Revoga um convite pendente; convite já usado ou revogado não muda. */
	public void revoke(Instant now) {
		if (usedAt == null && revokedAt == null) {
			this.revokedAt = now;
		}
	}

	public UUID getId() {
		return id;
	}

	public UUID getAthleteId() {
		return athleteId;
	}

	public UUID getCoachId() {
		return coachId;
	}

	public String getEmail() {
		return email;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getUsedAt() {
		return usedAt;
	}

	public Instant getRevokedAt() {
		return revokedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
