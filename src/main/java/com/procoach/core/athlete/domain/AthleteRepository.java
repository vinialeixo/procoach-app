package com.procoach.core.athlete.domain;

import java.util.Optional;

/**
 * Porta de persistência do agregado Athlete (T-017). Implementada na camada
 * de infraestrutura; o domínio depende apenas desta interface.
 */
public interface AthleteRepository {

	Athlete save(Athlete athlete);

	Optional<Athlete> findById(AthleteId id);

	boolean existsById(AthleteId id);
}
