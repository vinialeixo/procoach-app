package com.fitness.procoach.athlete.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de persistência do agregado Athlete (T-017). Implementada na camada
 * de infraestrutura; o domínio depende apenas desta interface.
 */
public interface AthleteRepository {

	Athlete save(Athlete athlete);

	Optional<Athlete> findById(AthleteId id);

	boolean existsById(AthleteId id);

	/** Atletas não removidos (soft-delete) da carteira do coach. */
	List<Athlete> findActiveByCoachId(UUID coachId);
}
