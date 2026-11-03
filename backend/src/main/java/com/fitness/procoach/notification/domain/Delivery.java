package com.fitness.procoach.notification.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Rastro da entrega de um aviso por um canal externo. Cada tentativa soma ao contador; falha
 * pode ser repetida até {@link #MAX_ATTEMPTS}, e entrega concluída ou sem destino não se repete.
 */
public final class Delivery {

	/** Limite de tentativas por aviso e canal. */
	public static final int MAX_ATTEMPTS = 3;

	private static final int MAX_ERROR_LENGTH = 500;

	private final UUID id;
	private final UUID notificationId;
	private final NotificationChannelType channel;
	private DeliveryStatus status;
	private int attempts;
	private String lastError;
	private final Instant createdAt;
	private Instant updatedAt;

	private Delivery(UUID id, UUID notificationId, NotificationChannelType channel, DeliveryStatus status,
			int attempts, String lastError, Instant createdAt, Instant updatedAt) {
		this.id = Objects.requireNonNull(id, "id");
		this.notificationId = Objects.requireNonNull(notificationId, "notificationId");
		this.channel = Objects.requireNonNull(channel, "channel");
		this.status = Objects.requireNonNull(status, "status");
		this.attempts = attempts;
		this.lastError = lastError;
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
	}

	public static Delivery start(UUID notificationId, NotificationChannelType channel, Instant now) {
		return new Delivery(UUID.randomUUID(), notificationId, channel, DeliveryStatus.PENDING, 0, null, now, now);
	}

	public static Delivery reconstitute(UUID id, UUID notificationId, NotificationChannelType channel,
			DeliveryStatus status, int attempts, String lastError, Instant createdAt, Instant updatedAt) {
		return new Delivery(id, notificationId, channel, status, attempts, lastError, createdAt, updatedAt);
	}

	/** Concluída (enviada ou sem destino): não há o que tentar de novo. */
	public boolean isFinal() {
		return status == DeliveryStatus.SENT || status == DeliveryStatus.SKIPPED;
	}

	/** Falhou e ainda tem tentativas sobrando. */
	public boolean canRetry() {
		return status == DeliveryStatus.FAILED && attempts < MAX_ATTEMPTS;
	}

	public void markSent(Instant now) {
		attempt(DeliveryStatus.SENT, null, now);
	}

	public void markSkipped(Instant now, String reason) {
		attempt(DeliveryStatus.SKIPPED, reason, now);
	}

	public void markFailed(Instant now, String error) {
		attempt(DeliveryStatus.FAILED, error, now);
	}

	private void attempt(DeliveryStatus outcome, String detail, Instant now) {
		if (isFinal()) {
			throw new IllegalStateException("Delivery already finished: " + status);
		}
		this.attempts++;
		this.status = outcome;
		this.lastError = detail == null ? null
				: (detail.length() > MAX_ERROR_LENGTH ? detail.substring(0, MAX_ERROR_LENGTH) : detail);
		this.updatedAt = Objects.requireNonNull(now, "now");
	}

	public UUID getId() {
		return id;
	}

	public UUID getNotificationId() {
		return notificationId;
	}

	public NotificationChannelType getChannel() {
		return channel;
	}

	public DeliveryStatus getStatus() {
		return status;
	}

	public int getAttempts() {
		return attempts;
	}

	public String getLastError() {
		return lastError;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
