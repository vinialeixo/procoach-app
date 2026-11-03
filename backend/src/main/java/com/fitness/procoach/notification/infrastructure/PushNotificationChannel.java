package com.fitness.procoach.notification.infrastructure;

import com.fitness.procoach.notification.application.NotificationChannel;
import com.fitness.procoach.notification.domain.DeviceToken;
import com.fitness.procoach.notification.domain.DeviceTokenRepository;
import com.fitness.procoach.notification.domain.Notification;
import com.fitness.procoach.notification.domain.NotificationChannelType;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Push para os dispositivos registrados do atleta. Indisponível enquanto não houver um
 * {@link PushGateway} configurado. Se alguns dispositivos falham e outros recebem, o aviso conta
 * como enviado; se todos falham, a tentativa falha e é repetida depois.
 */
@Component
public class PushNotificationChannel implements NotificationChannel {

	static final String TITLE = "ProCoach";

	private final ObjectProvider<PushGateway> gateway;
	private final DeviceTokenRepository devices;

	public PushNotificationChannel(ObjectProvider<PushGateway> gateway, DeviceTokenRepository devices) {
		this.gateway = gateway;
		this.devices = devices;
	}

	@Override
	public NotificationChannelType type() {
		return NotificationChannelType.PUSH;
	}

	@Override
	public boolean available() {
		return gateway.getIfAvailable() != null;
	}

	@Override
	public Result send(Notification notification) {
		List<DeviceToken> targets = devices.findByAthleteId(notification.getAthleteId());
		if (targets.isEmpty()) {
			return Result.skipped("atleta sem dispositivo registrado");
		}
		PushGateway push = gateway.getObject();
		List<String> errors = new ArrayList<>();
		int delivered = 0;
		for (DeviceToken device : targets) {
			try {
				push.send(device.getToken(), TITLE, notification.getMessage());
				delivered++;
			}
			catch (RuntimeException ex) {
				errors.add(ex.getClass().getSimpleName() + ": " + ex.getMessage());
			}
		}
		if (delivered == 0) {
			throw new IllegalStateException("push recusado em todos os dispositivos: " + String.join("; ", errors));
		}
		return Result.sent();
	}
}
