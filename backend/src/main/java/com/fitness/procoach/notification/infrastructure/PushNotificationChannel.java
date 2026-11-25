package com.fitness.procoach.notification.infrastructure;

import com.fitness.procoach.audit.application.AuditService;
import com.fitness.procoach.notification.application.NotificationChannel;
import com.fitness.procoach.notification.domain.DeviceToken;
import com.fitness.procoach.notification.domain.DeviceTokenRepository;
import com.fitness.procoach.notification.domain.Notification;
import com.fitness.procoach.notification.domain.NotificationChannelType;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Push para os dispositivos registrados do atleta. Indisponível enquanto não houver um
 * {@link PushGateway} configurado. Se alguns dispositivos falham e outros recebem, o aviso conta
 * como enviado; se todos falham, a tentativa falha e é repetida depois. Token recusado como
 * definitivo pelo provedor é removido em vez de tentado de novo.
 */
@Component
public class PushNotificationChannel implements NotificationChannel {

	static final String TITLE = "ProCoach";

	private static final Logger log = LoggerFactory.getLogger(PushNotificationChannel.class);

	private final ObjectProvider<PushGateway> gateway;
	private final DeviceTokenRepository devices;
	private final Consumer<DeviceToken> auditRemoval;

	@Autowired
	public PushNotificationChannel(ObjectProvider<PushGateway> gateway, DeviceTokenRepository devices,
			AuditService audit) {
		this(gateway, devices, device -> audit.record("DeviceToken", device.getId(), device.getAthleteId(),
				"DEVICE_TOKEN_REMOVED", null, "{\"reason\":\"invalid_token\"}"));
	}

	PushNotificationChannel(ObjectProvider<PushGateway> gateway, DeviceTokenRepository devices) {
		this(gateway, devices, device -> {
		});
	}

	PushNotificationChannel(ObjectProvider<PushGateway> gateway, DeviceTokenRepository devices,
			Consumer<DeviceToken> auditRemoval) {
		this.gateway = gateway;
		this.devices = devices;
		this.auditRemoval = auditRemoval;
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
		int removed = 0;
		for (DeviceToken device : targets) {
			try {
				push.send(device.getToken(), TITLE, notification.getMessage());
				delivered++;
			}
			catch (InvalidPushTokenException ex) {
				log.info("Removendo dispositivo {} do atleta {}: {}", device.getId(), device.getAthleteId(),
						ex.getMessage());
				devices.delete(device);
				auditRemoval.accept(device);
				removed++;
			}
			catch (RuntimeException ex) {
				errors.add(ex.getClass().getSimpleName() + ": " + ex.getMessage());
			}
		}
		if (delivered == 0) {
			if (errors.isEmpty()) {
				return Result.skipped(removed + " dispositivo(s) com token inválido removido(s)");
			}
			throw new IllegalStateException("push recusado em todos os dispositivos: " + String.join("; ", errors));
		}
		return Result.sent();
	}
}
