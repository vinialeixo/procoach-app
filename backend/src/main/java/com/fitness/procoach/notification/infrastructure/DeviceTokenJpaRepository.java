package com.fitness.procoach.notification.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceTokenJpaRepository extends JpaRepository<DeviceTokenJpaEntity, UUID> {

	Optional<DeviceTokenJpaEntity> findByToken(String token);

	List<DeviceTokenJpaEntity> findByAthleteId(UUID athleteId);
}
