package com.procoach.core.security.application;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.procoach.core.security.domain.Permission;
import com.procoach.core.security.domain.Role;
import com.procoach.core.security.domain.RoleRepository;

@Service
public class RoleService {

	private final RoleRepository roleRepository;

	public RoleService(RoleRepository roleRepository) {
		this.roleRepository = roleRepository;
	}

	@Transactional
	public Role createRole(String name, Set<Permission> permissions) {
		Role role = Role.create(name, permissions);
		return roleRepository.save(role);
	}

	@Transactional
	public Role addPermission(UUID roleId, Permission permission) {
		Role role = roleRepository.findById(roleId)
				.orElseThrow(() -> new IllegalArgumentException("Role inexistente: " + roleId));
		role.addPermission(permission);
		return roleRepository.save(role);
	}

	@Transactional(readOnly = true)
	public Optional<Role> findById(UUID id) {
		return roleRepository.findById(id);
	}

	@Transactional(readOnly = true)
	public List<Role> findAll() {
		return roleRepository.findAll();
	}
}
