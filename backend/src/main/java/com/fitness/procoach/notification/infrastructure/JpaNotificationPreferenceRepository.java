package com.fitness.procoach.notification.infrastructure;

import com.fitness.procoach.notification.domain.NotificationPreference;
import com.fitness.procoach.notification.domain.NotificationPreferenceRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaNotificationPreferenceRepository implements NotificationPreferenceRepository {

	private final NotificationPreferenceJpaRepository jpaRepository;

	public JpaNotificationPreferenceRepository(NotificationPreferenceJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<NotificationPreference> findByAthleteId(UUID athleteId) {
		return jpaRepository.findById(athleteId).map(JpaNotificationPreferenceRepository::toDomain);
	}

	@Override
	@Transactional
	public NotificationPreference save(NotificationPreference preference) {
		NotificationPreferenceJpaEntity entity = new NotificationPreferenceJpaEntity();
		entity.setAthleteId(preference.getAthleteId());
		entity.setEmailEnabled(preference.isEmailEnabled());
		entity.setPushEnabled(preference.isPushEnabled());
		entity.setUpdatedAt(preference.getUpdatedAt());
		return toDomain(jpaRepository.save(entity));
	}

	private static NotificationPreference toDomain(NotificationPreferenceJpaEntity e) {
		return NotificationPreference.of(e.getAthleteId(), e.isEmailEnabled(), e.isPushEnabled(), e.getUpdatedAt());
	}
}
