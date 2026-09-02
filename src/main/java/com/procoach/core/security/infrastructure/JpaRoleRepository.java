package com.procoach.core.security.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.procoach.core.security.domain.Role;
import com.procoach.core.security.domain.RoleRepository;

@Repository
public class JpaRoleRepository implements RoleRepository {

	private final RoleJpaRepository jpaRepository;

	public JpaRoleRepository(RoleJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	@Transactional
	public Role save(Role role) {
		return RoleMapper.toDomain(jpaRepository.save(RoleMapper.toEntity(role)));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Role> findById(UUID id) {
		return jpaRepository.findById(id).map(RoleMapper::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Role> findByName(String name) {
		return jpaRepository.findByName(name).map(RoleMapper::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Role> findAll() {
		return jpaRepository.findAll().stream().map(RoleMapper::toDomain).toList();
	}
}
