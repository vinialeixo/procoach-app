package com.fitness.procoach.nutrition.domain;

import java.util.List;
import java.util.Optional;

/**
 * Porta de persistência do agregado Diet. Implementada na camada de infraestrutura.
 */
public interface DietRepository {

    Diet save(Diet diet);

    Optional<Diet> findById(DietId id);

    List<Diet> findAllVersionsByAthleteId(AthleteId athleteId);

    Optional<Diet> findLatestByAthleteId(AthleteId athleteId);
}
