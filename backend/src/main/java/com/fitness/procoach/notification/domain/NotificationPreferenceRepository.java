package com.fitness.procoach.notification.domain;

import java.util.Optional;
import java.util.UUID;

public interface NotificationPreferenceRepository {

	Optional<NotificationPreference> findByAthleteId(UUID athleteId);

	NotificationPreference save(NotificationPreference preference);
}
