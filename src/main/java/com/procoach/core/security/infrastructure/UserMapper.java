package com.procoach.core.security.infrastructure;

import com.procoach.core.security.domain.User;

final class UserMapper {

	private UserMapper() {
	}

	static UserJpaEntity toEntity(User user) {
		UserJpaEntity entity = new UserJpaEntity();
		entity.setId(user.getId());
		entity.setUsername(user.getUsername());
		entity.setEmail(user.getEmail());
		entity.setPasswordHash(user.getPasswordHash());
		entity.setEnabled(user.isEnabled());
		entity.setRoleIds(new java.util.HashSet<>(user.getRoleIds()));
		entity.setCreatedAt(user.getCreatedAt());
		entity.setUpdatedAt(user.getUpdatedAt());
		entity.setVersion(user.getVersion());
		return entity;
	}

	static User toDomain(UserJpaEntity entity) {
		return User.reconstruct(entity.getId(), entity.getUsername(), entity.getEmail(), entity.getPasswordHash(),
				entity.isEnabled(), entity.getRoleIds(), entity.getCreatedAt(), entity.getUpdatedAt(),
				entity.getVersion());
	}
}
