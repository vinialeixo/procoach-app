package com.fitness.procoach.notification.application;

import com.fitness.procoach.notification.domain.Delivery;
import com.fitness.procoach.notification.domain.DeliveryRepository;
import com.fitness.procoach.notification.domain.Notification;
import com.fitness.procoach.notification.domain.NotificationPreference;
import com.fitness.procoach.notification.domain.NotificationPreferenceRepository;
import com.fitness.procoach.notification.domain.NotificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Entrega os avisos pelos canais externos (e-mail, push) respeitando a preferência do atleta e
 * registrando cada tentativa. Falha de um canal nunca derruba a ação do coach nem o outro canal:
 * a tentativa fica como FAILED e é repetida por {@link #retryFailed()} até o limite.
 */
@Service
public class NotificationDispatcher {

	private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

	private final NotificationRepository notifications;
	private final NotificationPreferenceRepository preferences;
	private final DeliveryRepository deliveries;
	private final List<NotificationChannel> channels;

	public NotificationDispatcher(NotificationRepository notifications, NotificationPreferenceRepository preferences,
			DeliveryRepository deliveries, List<NotificationChannel> channels) {
		this.notifications = notifications;
		this.preferences = preferences;
		this.deliveries = deliveries;
		this.channels = channels;
	}

	/**
	 * Primeira entrega de um aviso. Transação própria: roda depois do commit da ação do coach.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void dispatch(UUID notificationId) {
		Optional<Notification> found = notifications.findById(notificationId);
		if (found.isEmpty()) {
			return;
		}
		Notification notification = found.get();
		NotificationPreference preference = preferences.findByAthleteId(notification.getAthleteId())
				.orElse(NotificationPreference.defaults(notification.getAthleteId()));
		for (NotificationChannel channel : channels) {
			if (!channel.available() || !preference.allows(channel.type())) {
				continue;
			}
			Delivery delivery = deliveries.find(notification.getId(), channel.type())
					.orElseGet(() -> Delivery.start(notification.getId(), channel.type(), Instant.now()));
			if (!delivery.isFinal()) {
				attempt(notification, channel, delivery);
			}
		}
	}

	/**
	 * Repete as entregas que falharam e ainda têm tentativas sobrando. Devolve quantas foram
	 * tentadas. Respeita a preferência atual do atleta: quem desligou o canal não recebe a repetição.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public int retryFailed() {
		int attempted = 0;
		for (Delivery delivery : deliveries.findRetryable(Delivery.MAX_ATTEMPTS)) {
			Notification notification = notifications.findById(delivery.getNotificationId()).orElse(null);
			NotificationChannel channel = channels.stream()
				.filter(c -> c.type() == delivery.getChannel() && c.available())
				.findFirst()
				.orElse(null);
			if (notification == null || channel == null) {
				continue;
			}
			NotificationPreference preference = preferences.findByAthleteId(notification.getAthleteId())
				.orElse(NotificationPreference.defaults(notification.getAthleteId()));
			if (!preference.allows(channel.type())) {
				continue;
			}
			attempt(notification, channel, delivery);
			attempted++;
		}
		return attempted;
	}

	private void attempt(Notification notification, NotificationChannel channel, Delivery delivery) {
		Instant now = Instant.now();
		try {
			NotificationChannel.Result result = channel.send(notification);
			if (result.skipped()) {
				delivery.markSkipped(now, result.detail());
			}
			else {
				delivery.markSent(now);
			}
		}
		catch (Exception ex) {
			log.warn("Falha ao entregar o aviso {} por {}: {}", notification.getId(), channel.type(), ex.toString());
			delivery.markFailed(now, ex.getClass().getSimpleName() + ": " + ex.getMessage());
		}
		deliveries.save(delivery);
	}
}
