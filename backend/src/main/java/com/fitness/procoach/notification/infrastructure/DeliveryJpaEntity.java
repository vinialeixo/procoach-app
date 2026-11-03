package com.fitness.procoach.notification.infrastructure;

import com.fitness.procoach.notification.domain.DeliveryStatus;
import com.fitness.procoach.notification.domain.NotificationChannelType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_delivery",
		uniqueConstraints = @UniqueConstraint(name = "uq_notification_delivery",
				columnNames = { "notification_id", "channel" }))
public class DeliveryJpaEntity {

	@Id
	private UUID id;

	@Column(name = "notification_id", nullable = false)
	private UUID notificationId;

	@Enumerated(EnumType.STRING)
	@Column(name = "channel", nullable = false, length = 20)
	private NotificationChannelType channel;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private DeliveryStatus status;

	@Column(name = "attempts", nullable = false)
	private int attempts;

	@Column(name = "last_error", length = 500)
	private String lastError;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public UUID getNotificationId() {
		return notificationId;
	}

	public void setNotificationId(UUID notificationId) {
		this.notificationId = notificationId;
	}

	public NotificationChannelType getChannel() {
		return channel;
	}

	public void setChannel(NotificationChannelType channel) {
		this.channel = channel;
	}

	public DeliveryStatus getStatus() {
		return status;
	}

	public void setStatus(DeliveryStatus status) {
		this.status = status;
	}

	public int getAttempts() {
		return attempts;
	}

	public void setAttempts(int attempts) {
		this.attempts = attempts;
	}

	public String getLastError() {
		return lastError;
	}

	public void setLastError(String lastError) {
		this.lastError = lastError;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}
