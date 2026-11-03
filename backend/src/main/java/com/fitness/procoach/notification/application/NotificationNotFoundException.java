package com.fitness.procoach.notification.application;

import java.util.UUID;

/** Aviso inexistente ou de outro atleta — 404, sem vazar a existência. */
public class NotificationNotFoundException extends RuntimeException {

	public NotificationNotFoundException(UUID id) {
		super("Notification not found: " + id);
	}
}
