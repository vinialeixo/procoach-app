package com.procoach.core.security.domain;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Papel (Role) do RBAC. Agregado raiz com repositório próprio.
 */
public class Role {

	private final UUID id;
	private String name;
	private final Set<Permission> permissions;
	private boolean active;

	private Role(UUID id, String name, Set<Permission> permissions, boolean active) {
		this.id = id;
		this.name = name;
		this.permissions = new HashSet<>(permissions);
		this.active = active;
	}

	public static Role create(String name, Set<Permission> permissions) {
		return new Role(UUID.randomUUID(), name, permissions, true);
	}

	public static Role reconstruct(UUID id, String name, Set<Permission> permissions, boolean active) {
		return new Role(id, name, permissions, active);
	}

	public void addPermission(Permission permission) {
		this.permissions.add(permission);
	}

	public void removePermission(Permission permission) {
		this.permissions.remove(permission);
	}

	public void deactivate() {
		this.active = false;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Set<Permission> getPermissions() {
		return Collections.unmodifiableSet(permissions);
	}

	public boolean isActive() {
		return active;
	}
}
