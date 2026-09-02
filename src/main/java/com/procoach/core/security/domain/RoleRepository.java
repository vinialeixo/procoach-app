package com.procoach.core.security.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository {

	Role save(Role role);

	Optional<Role> findById(UUID id);

	Optional<Role> findByName(String name);

	List<Role> findAll();
}
