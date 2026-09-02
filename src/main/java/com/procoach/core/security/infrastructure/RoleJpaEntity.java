package com.procoach.core.security.infrastructure;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.procoach.core.security.domain.Permission;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "role")
public class RoleJpaEntity {

	@Id
	private UUID id;

	@Column(nullable = false, unique = true, length = 100)
	private String name;

	@ElementCollection(targetClass = Permission.class)
	@CollectionTable(name = "role_permission", joinColumns = @JoinColumn(name = "role_id"))
	@Enumerated(EnumType.STRING)
	@Column(name = "permission", length = 50)
	private Set<Permission> permissions = new HashSet<>();

	@Column(nullable = false)
	private boolean active;

	protected RoleJpaEntity() {
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<Permission> getPermissions() {
		return permissions;
	}

	public void setPermissions(Set<Permission> permissions) {
		this.permissions = permissions;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
}
