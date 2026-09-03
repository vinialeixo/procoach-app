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
	private final Instant createdAt;
	private Instant updatedAt;
	private long version;

	private User(UUID id, String username, String email, String passwordHash, boolean enabled, Set<UUID> roleIds,
			Instant createdAt, Instant updatedAt, long version) {
		this.id = id;
		this.username = username;
		this.email = email;
		this.passwordHash = passwordHash;
		this.enabled = enabled;
		this.roleIds = new HashSet<>(roleIds);
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.version = version;
	}

	public static User create(String username, String email, String passwordHash, Set<UUID> roleIds) {
		Instant now = Instant.now();
		return new User(UUID.randomUUID(), username, email, passwordHash, true, roleIds, now, now, 0);
	}

	public static User reconstruct(UUID id, String username, String email, String passwordHash, boolean enabled,
			Set<UUID> roleIds, Instant createdAt, Instant updatedAt, long version) {
		return new User(id, username, email, passwordHash, enabled, roleIds, createdAt, updatedAt, version);
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
