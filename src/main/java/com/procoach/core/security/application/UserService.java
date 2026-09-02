package com.procoach.core.security.application;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.procoach.core.security.domain.Role;
import com.procoach.core.security.domain.RoleRepository;
import com.procoach.core.security.domain.User;
import com.procoach.core.security.domain.UserRepository;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;

	public UserService(UserRepository userRepository, RoleRepository roleRepository) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
	}

	@Transactional
	public User createUser(String username, String email, String passwordHash, Set<UUID> roleIds) {
		for (UUID roleId : roleIds) {
			if (roleRepository.findById(roleId).isEmpty()) {
				throw new IllegalArgumentException("Role inexistente: " + roleId);
			}
		}
		User user = User.create(username, email, passwordHash, roleIds);
		return userRepository.save(user);
	}

	@Transactional
	public User assignRole(UUID userId, UUID roleId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("Usuário inexistente: " + userId));
		if (roleRepository.findById(roleId).isEmpty()) {
			throw new IllegalArgumentException("Role inexistente: " + roleId);
		}
		user.assignRole(roleId);
		return userRepository.save(user);
	}

	@Transactional(readOnly = true)
	public Optional<User> findById(UUID id) {
		return userRepository.findById(id);
	}

	@Transactional(readOnly = true)
	public List<User> findAll() {
		return userRepository.findAll();
	}
}
