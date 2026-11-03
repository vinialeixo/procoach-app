package com.fitness.procoach.notification.infrastructure;

import com.fitness.procoach.notification.domain.Delivery;
import com.fitness.procoach.notification.domain.DeliveryRepository;
import com.fitness.procoach.notification.domain.DeliveryStatus;
import com.fitness.procoach.notification.domain.NotificationChannelType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaDeliveryRepository implements DeliveryRepository {

	private final DeliveryJpaRepository jpaRepository;

	public JpaDeliveryRepository(DeliveryJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	@Transactional
	public Delivery save(Delivery delivery) {
		DeliveryJpaEntity entity = new DeliveryJpaEntity();
		entity.setId(delivery.getId());
		entity.setNotificationId(delivery.getNotificationId());
		entity.setChannel(delivery.getChannel());
		entity.setStatus(delivery.getStatus());
		entity.setAttempts(delivery.getAttempts());
		entity.setLastError(delivery.getLastError());
		entity.setCreatedAt(delivery.getCreatedAt());
		entity.setUpdatedAt(delivery.getUpdatedAt());
		return toDomain(jpaRepository.save(entity));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Delivery> find(UUID notificationId, NotificationChannelType channel) {
		return jpaRepository.findByNotificationIdAndChannel(notificationId, channel)
				.map(JpaDeliveryRepository::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Delivery> findRetryable(int maxAttempts) {
		return jpaRepository.findByStatusAndAttemptsLessThan(DeliveryStatus.FAILED, maxAttempts).stream()
				.map(JpaDeliveryRepository::toDomain)
				.toList();
	}

	private static Delivery toDomain(DeliveryJpaEntity e) {
		return Delivery.reconstitute(e.getId(), e.getNotificationId(), e.getChannel(), e.getStatus(), e.getAttempts(),
				e.getLastError(), e.getCreatedAt(), e.getUpdatedAt());
	}
}
