package com.fitness.procoach.security.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Porta de persistência do convite do atleta. */
public interface AthleteInviteRepository {

	AthleteInvite save(AthleteInvite invite);

	/** Busca pelo hash do token travando a linha (aceite concorrente do mesmo convite). */
	Optional<AthleteInvite> findByTokenHashForUpdate(String tokenHash);

	Optional<AthleteInvite> findByTokenHash(String tokenHash);

	/** Convites ainda não usados nem revogados do atleta (podem estar expirados). */
	List<AthleteInvite> findOpenByAthleteId(UUID athleteId);
}
