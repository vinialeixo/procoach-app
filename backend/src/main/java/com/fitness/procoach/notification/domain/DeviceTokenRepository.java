package com.fitness.procoach.notification.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceTokenRepository {

	DeviceToken save(DeviceToken device);

	/** Registro ativo do token (removidos não aparecem). */
	Optional<DeviceToken> findByToken(String token);

	/** Inclui registros removidos: o cadastro repetido reativa o mesmo token. */
	default Optional<DeviceToken> findByTokenIncludingDeleted(String token) {
		return findByToken(token);
	}

	List<DeviceToken> findByAthleteId(UUID athleteId);

	/** Remoção lógica: o registro é marcado, nunca apagado fisicamente. */
	void delete(DeviceToken device);
}
