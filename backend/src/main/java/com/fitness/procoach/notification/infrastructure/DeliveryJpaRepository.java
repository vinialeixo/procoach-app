package com.fitness.procoach.notification.infrastructure;

import com.fitness.procoach.notification.domain.DeliveryStatus;
import com.fitness.procoach.notification.domain.NotificationChannelType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryJpaRepository extends JpaRepository<DeliveryJpaEntity, UUID> {

	Optional<DeliveryJpaEntity> findByNotificationIdAndChannel(UUID notificationId, NotificationChannelType channel);

	List<DeliveryJpaEntity> findByStatusAndAttemptsLessThan(DeliveryStatus status, int attempts);
}
