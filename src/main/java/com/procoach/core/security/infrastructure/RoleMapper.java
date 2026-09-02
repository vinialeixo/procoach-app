package com.procoach.core.security.infrastructure;

import com.procoach.core.security.domain.Role;

final class RoleMapper {

	private RoleMapper() {
	}

	static RoleJpaEntity toEntity(Role role) {
		RoleJpaEntity entity = new RoleJpaEntity();
		entity.setId(role.getId());
		entity.setName(role.getName());
		entity.setPermissions(new java.util.HashSet<>(role.getPermissions()));
		entity.setActive(role.isActive());
		return entity;
	}

	static Role toDomain(RoleJpaEntity entity) {
		return Role.reconstruct(entity.getId(), entity.getName(), entity.getPermissions(), entity.isActive());
	}
}
