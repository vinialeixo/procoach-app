package com.fitness.procoach.security.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

	User save(User user);

	Optional<User> findById(UUID id);

	Optional<User> findByUsername(String username);

	Optional<User> findByEmail(String email);

	List<User> findAll();

	/** Já existe credencial vinculada a este atleta? */
	boolean existsByAthleteId(UUID athleteId);

	/** Credencial vinculada ao atleta, se ele já criou o acesso. */
	Optional<User> findByAthleteId(UUID athleteId);
}
