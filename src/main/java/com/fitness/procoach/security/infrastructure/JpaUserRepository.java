package com.fitness.procoach.security.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.fitness.procoach.security.domain.User;
import com.fitness.procoach.security.domain.UserRepository;

@Repository
public class JpaUserRepository implements UserRepository {

	private final UserJpaRepository jpaRepository;

	public JpaUserRepository(UserJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	@Transactional
	public User save(User user) {
		return UserMapper.toDomain(jpaRepository.save(UserMapper.toEntity(user)));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<User> findById(UUID id) {
		return jpaRepository.findById(id).map(UserMapper::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<User> findByUsername(String username) {
		return jpaRepository.findByUsername(username).map(UserMapper::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<User> findByEmail(String email) {
		return jpaRepository.findByEmail(email).map(UserMapper::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public List<User> findAll() {
		return jpaRepository.findAll().stream().map(UserMapper::toDomain).toList();
	}
}
