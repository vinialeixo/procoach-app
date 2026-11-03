package com.fitness.procoach.security.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

	Optional<UserJpaEntity> findByUsername(String username);

	Optional<UserJpaEntity> findByEmail(String email);

	boolean existsByAthleteId(UUID athleteId);
}
