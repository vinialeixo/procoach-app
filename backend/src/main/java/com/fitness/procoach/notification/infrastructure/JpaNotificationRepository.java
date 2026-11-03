package com.fitness.procoach.notification.infrastructure;

import com.fitness.procoach.notification.domain.Notification;
import com.fitness.procoach.notification.domain.NotificationRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaNotificationRepository implements NotificationRepository {

	private final NotificationJpaRepository jpaRepository;

	public JpaNotificationRepository(NotificationJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	@Transactional
	public Notification save(Notification notification) {
		return toDomain(jpaRepository.save(toEntity(notification)));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Notification> findById(UUID id) {
		return jpaRepository.findById(id).map(JpaNotificationRepository::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Notification> findByAthleteId(UUID athleteId, boolean unreadOnly) {
		List<NotificationJpaEntity> rows = unreadOnly
				? jpaRepository.findByAthleteIdAndReadAtIsNullOrderByCreatedAtDesc(athleteId)
				: jpaRepository.findByAthleteIdOrderByCreatedAtDesc(athleteId);
		return rows.stream().map(JpaNotificationRepository::toDomain).toList();
	}

	private static NotificationJpaEntity toEntity(Notification n) {
		NotificationJpaEntity entity = new NotificationJpaEntity();
		entity.setId(n.getId());
		entity.setAthleteId(n.getAthleteId());
		entity.setType(n.getType());
		entity.setMessage(n.getMessage());
		entity.setReferenceType(n.getReferenceType());
		entity.setReferenceId(n.getReferenceId());
		entity.setCreatedAt(n.getCreatedAt());
		entity.setReadAt(n.getReadAt());
		return entity;
	}

	private static Notification toDomain(NotificationJpaEntity e) {
		return Notification.reconstitute(e.getId(), e.getAthleteId(), e.getType(), e.getMessage(),
				e.getReferenceType(), e.getReferenceId(), e.getCreatedAt(), e.getReadAt());
	}
}
