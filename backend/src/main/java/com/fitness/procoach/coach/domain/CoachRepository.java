package com.fitness.procoach.coach.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * Porta de persistência do agregado Coach. A implementação vive na camada de
 * infraestrutura.
 */
public interface CoachRepository {

	Coach save(Coach coach);

	Optional<Coach> findById(CoachId id);

	/** Resolve o Coach vinculado ao usuário autenticável (User.id — subject do JWT). */
	Optional<Coach> findByUserId(UUID userId);

	boolean existsByUserId(UUID userId);
}
