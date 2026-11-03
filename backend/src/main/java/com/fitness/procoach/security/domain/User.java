package com.fitness.procoach.security.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Usuário do sistema. Agregado raiz. Referencia papéis por UUID (nunca objeto
 * aninhado), conforme regra de referências entre agregados.
 */
public class User {

	private final UUID id;
	private String username;
	private String email;
	private String passwordHash;
	private boolean enabled;
	private final Set<UUID> roleIds;
	private UUID athleteId;
	private boolean mfaEnabled;
	private String mfaSecret;
	private final Instant createdAt;
	private Instant updatedAt;
	private long version;

	private User(UUID id, String username, String email, String passwordHash, boolean enabled, Set<UUID> roleIds,
			UUID athleteId, boolean mfaEnabled, String mfaSecret, Instant createdAt, Instant updatedAt, long version) {
		this.id = id;
		this.username = username;
		this.email = email;
		this.passwordHash = passwordHash;
		this.enabled = enabled;
		this.roleIds = new HashSet<>(roleIds);
		this.athleteId = athleteId;
		this.mfaEnabled = mfaEnabled;
		this.mfaSecret = mfaSecret;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.version = version;
	}

	public static User create(String username, String email, String passwordHash, Set<UUID> roleIds) {
		Instant now = Instant.now();
		return new User(UUID.randomUUID(), username, email, passwordHash, true, roleIds, null, false, null, now, now, 0);
	}

	public static User reconstruct(UUID id, String username, String email, String passwordHash, boolean enabled,
			Set<UUID> roleIds, UUID athleteId, boolean mfaEnabled, String mfaSecret, Instant createdAt,
			Instant updatedAt, long version) {
		return new User(id, username, email, passwordHash, enabled, roleIds, athleteId, mfaEnabled, mfaSecret,
				createdAt, updatedAt, version);
	}

	public void assignRole(UUID roleId) {
		this.roleIds.add(roleId);
		this.updatedAt = Instant.now();
	}

	public void removeRole(UUID roleId) {
		this.roleIds.remove(roleId);
		this.updatedAt = Instant.now();
	}

	public void disable() {
		this.enabled = false;
		this.updatedAt = Instant.now();
	}

	/**
	 * Vincula a credencial ao agregado Athlete (referência por UUID). Só faz
	 * sentido para usuários com papel ATHLETE — é o claim que ancora o
	 * isolamento de dados da US-038/T-010.
	 */
	public void linkAthlete(UUID athleteId) {
		if (athleteId == null) {
			throw new IllegalArgumentException("athleteId não pode ser nulo");
		}
		this.athleteId = athleteId;
		this.updatedAt = Instant.now();
	}

	/**
	 * Inicia a ativação do MFA: guarda o segredo TOTP pendente de confirmação.
	 * O MFA só passa a valer (mfaEnabled=true) após o primeiro código válido.
	 */
	public void startMfaSetup(String secret) {
		if (secret == null || secret.isBlank()) {
			throw new IllegalArgumentException("Segredo TOTP não pode ser vazio");
		}
		this.mfaSecret = secret;
		this.mfaEnabled = false;
		this.updatedAt = Instant.now();
	}

	public void confirmMfaSetup() {
		if (this.mfaSecret == null) {
			throw new IllegalStateException("Nenhum setup de MFA em andamento");
		}
		this.mfaEnabled = true;
		this.updatedAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public Set<UUID> getRoleIds() {
		return Collections.unmodifiableSet(roleIds);
	}

	public UUID getAthleteId() {
		return athleteId;
	}

	public boolean isMfaEnabled() {
		return mfaEnabled;
	}

	public String getMfaSecret() {
		return mfaSecret;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public long getVersion() {
		return version;
	}
}
