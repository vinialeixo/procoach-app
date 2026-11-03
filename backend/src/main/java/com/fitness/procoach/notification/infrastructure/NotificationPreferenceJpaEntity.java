package com.fitness.procoach.notification.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_preference")
public class NotificationPreferenceJpaEntity {

	@Id
	@Column(name = "athlete_id")
	private UUID athleteId;

	@Column(name = "email_enabled", nullable = false)
	private boolean emailEnabled;

	@Column(name = "push_enabled", nullable = false)
	private boolean pushEnabled;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public UUID getAthleteId() {
		return athleteId;
	}

	public void setAthleteId(UUID athleteId) {
		this.athleteId = athleteId;
	}

	public boolean isEmailEnabled() {
		return emailEnabled;
	}

	public void setEmailEnabled(boolean emailEnabled) {
		this.emailEnabled = emailEnabled;
	}

	public boolean isPushEnabled() {
		return pushEnabled;
	}

	public void setPushEnabled(boolean pushEnabled) {
		this.pushEnabled = pushEnabled;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}
