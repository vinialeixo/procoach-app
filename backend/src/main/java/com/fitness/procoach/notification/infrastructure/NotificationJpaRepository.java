package com.fitness.procoach.notification.infrastructure;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, UUID> {

	List<NotificationJpaEntity> findByAthleteIdOrderByCreatedAtDesc(UUID athleteId);

	List<NotificationJpaEntity> findByAthleteIdAndReadAtIsNullOrderByCreatedAtDesc(UUID athleteId);
}
