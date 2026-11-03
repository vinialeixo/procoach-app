package com.fitness.procoach.notification.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceTokenRepository {

	DeviceToken save(DeviceToken device);

	Optional<DeviceToken> findByToken(String token);

	List<DeviceToken> findByAthleteId(UUID athleteId);

	void delete(DeviceToken device);
}
