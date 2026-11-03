package com.fitness.procoach.notification.application;

import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Dispara a entrega externa só depois do commit da ação do coach (aviso de algo que não foi
 * salvo seria mentira) e fora da requisição dele, para o SMTP lento não atrasar a resposta.
 */
@Component
class NotificationDeliveryListener {

	private static final Logger log = LoggerFactory.getLogger(NotificationDeliveryListener.class);

	private final NotificationDispatcher dispatcher;
	private final Executor executor;

	NotificationDeliveryListener(NotificationDispatcher dispatcher,
			@Qualifier("notificationExecutor") Executor executor) {
		this.dispatcher = dispatcher;
		this.executor = executor;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	void on(NotificationCreatedEvent event) {
		executor.execute(() -> {
			try {
				dispatcher.dispatch(event.notificationId());
			}
			catch (RuntimeException ex) {
				log.error("Entrega do aviso {} falhou", event.notificationId(), ex);
			}
		});
	}
}
