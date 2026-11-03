package com.fitness.procoach.notification.application;

import com.fitness.procoach.notification.domain.Notification;
import com.fitness.procoach.notification.domain.NotificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Avisos in-app ao atleta. Quem muda algo que o atleta enxerga (treino ativo editado, dieta
 * aprovada) chama {@link #notifyAthlete} dentro da própria transação — se a mudança falhar, o
 * aviso também não existe. O envio por e-mail ou push não faz parte deste serviço.
 */
@Service
public class NotificationService {

	private final NotificationRepository repository;

	private final ApplicationEventPublisher events;

	public NotificationService(NotificationRepository repository, ApplicationEventPublisher events) {
		this.repository = repository;
		this.events = events;
	}

	/** Visão de leitura de um aviso. */
	public record NotificationView(UUID id, String type, String message, String referenceType, UUID referenceId,
			Instant createdAt, boolean read, Instant readAt) {

		static NotificationView of(Notification n) {
			return new NotificationView(n.getId(), n.getType(), n.getMessage(), n.getReferenceType(),
					n.getReferenceId(), n.getCreatedAt(), n.isRead(), n.getReadAt());
		}
	}

	@Transactional
	public NotificationView notifyAthlete(UUID athleteId, String type, String message, String referenceType,
			UUID referenceId) {
		Notification saved = repository
				.save(Notification.create(athleteId, type, message, referenceType, referenceId, Instant.now()));
		// A entrega por e-mail/push só acontece depois do commit da ação que gerou o aviso.
		events.publishEvent(new NotificationCreatedEvent(saved.getId()));
		return NotificationView.of(saved);
	}

	/** Avisos do atleta, mais recentes primeiro. A autorização (o próprio atleta) é do chamador. */
	@Transactional(readOnly = true)
	public List<NotificationView> list(UUID athleteId, boolean unreadOnly) {
		return repository.findByAthleteId(athleteId, unreadOnly).stream().map(NotificationView::of).toList();
	}

	/** Marca um aviso do atleta como lido; aviso de outro atleta ou inexistente resulta em 404. */
	@Transactional
	public NotificationView markRead(UUID athleteId, UUID notificationId) {
		Notification notification = repository.findById(notificationId)
				.filter(n -> n.getAthleteId().equals(athleteId))
				.orElseThrow(() -> new NotificationNotFoundException(notificationId));
		notification.markRead(Instant.now());
		return NotificationView.of(repository.save(notification));
	}

	/** Marca todos os avisos não lidos do atleta; devolve quantos foram marcados. */
	@Transactional
	public int markAllRead(UUID athleteId) {
		Instant now = Instant.now();
		List<Notification> unread = repository.findByAthleteId(athleteId, true);
		for (Notification notification : unread) {
			notification.markRead(now);
			repository.save(notification);
		}
		return unread.size();
	}
}
