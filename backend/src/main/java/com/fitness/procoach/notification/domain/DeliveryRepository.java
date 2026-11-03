package com.fitness.procoach.notification.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryRepository {

	Delivery save(Delivery delivery);

	Optional<Delivery> find(UUID notificationId, NotificationChannelType channel);

	/** Entregas que falharam e ainda têm tentativas sobrando. */
	List<Delivery> findRetryable(int maxAttempts);
}
