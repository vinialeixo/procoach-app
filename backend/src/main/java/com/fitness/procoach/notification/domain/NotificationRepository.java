package com.fitness.procoach.notification.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Porta de persistência dos avisos ao atleta. */
public interface NotificationRepository {

	Notification save(Notification notification);

	Optional<Notification> findById(UUID id);

	/** Avisos do atleta, do mais recente para o mais antigo; opcionalmente só os não lidos. */
	List<Notification> findByAthleteId(UUID athleteId, boolean unreadOnly);
}
