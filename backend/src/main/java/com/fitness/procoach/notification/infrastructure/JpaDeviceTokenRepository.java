package com.fitness.procoach.notification.infrastructure;

import com.fitness.procoach.notification.domain.DeviceToken;
import com.fitness.procoach.notification.domain.DeviceTokenRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaDeviceTokenRepository implements DeviceTokenRepository {

	private final DeviceTokenJpaRepository jpaRepository;

	public JpaDeviceTokenRepository(DeviceTokenJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	@Transactional
	public DeviceToken save(DeviceToken device) {
		DeviceTokenJpaEntity entity = new DeviceTokenJpaEntity();
		entity.setId(device.getId());
		entity.setAthleteId(device.getAthleteId());
		entity.setToken(device.getToken());
		entity.setPlatform(device.getPlatform());
		entity.setCreatedAt(device.getCreatedAt());
		entity.setLastSeenAt(device.getLastSeenAt());
		return toDomain(jpaRepository.save(entity));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<DeviceToken> findByToken(String token) {
		return jpaRepository.findByToken(token).map(JpaDeviceTokenRepository::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public List<DeviceToken> findByAthleteId(UUID athleteId) {
		return jpaRepository.findByAthleteId(athleteId).stream().map(JpaDeviceTokenRepository::toDomain).toList();
	}

	@Override
	@Transactional
	public void delete(DeviceToken device) {
		jpaRepository.deleteById(device.getId());
	}

	private static DeviceToken toDomain(DeviceTokenJpaEntity e) {
		return DeviceToken.reconstitute(e.getId(), e.getAthleteId(), e.getToken(), e.getPlatform(), e.getCreatedAt(),
				e.getLastSeenAt());
	}
}
