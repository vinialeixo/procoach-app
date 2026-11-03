package com.fitness.procoach.notification.application;

import java.util.UUID;

/** Aviso gravado; dispara a entrega pelos canais externos depois do commit. */
public record NotificationCreatedEvent(UUID notificationId) {
}
