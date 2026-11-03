package com.fitness.procoach.notification.infrastructure;

import com.fitness.procoach.notification.domain.DevicePlatform;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_device")
public class DeviceTokenJpaEntity {

	@Id
	private UUID id;

	@Column(name = "athlete_id", nullable = false)
	private UUID athleteId;

	@Column(name = "token", nullable = false, unique = true, length = 512)
	private String token;

	@Enumerated(EnumType.STRING)
	@Column(name = "platform", nullable = false, length = 20)
	private DevicePlatform platform;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "last_seen_at", nullable = false)
	private Instant lastSeenAt;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public UUID getAthleteId() {
		return athleteId;
	}

	public void setAthleteId(UUID athleteId) {
		this.athleteId = athleteId;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public DevicePlatform getPlatform() {
		return platform;
	}

	public void setPlatform(DevicePlatform platform) {
		this.platform = platform;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getLastSeenAt() {
		return lastSeenAt;
	}

	public void setLastSeenAt(Instant lastSeenAt) {
		this.lastSeenAt = lastSeenAt;
	}
}
