package com.fitness.procoach.notification.infrastructure;

import com.fitness.procoach.notification.application.NotificationDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Repete periodicamente as entregas externas que falharam (até o limite de tentativas). */
@Component
@ConditionalOnProperty(name = "procoach.notification.retry.enabled", havingValue = "true", matchIfMissing = true)
public class NotificationRetryJob {

	private static final Logger log = LoggerFactory.getLogger(NotificationRetryJob.class);

	private final NotificationDispatcher dispatcher;

	public NotificationRetryJob(NotificationDispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}

	@Scheduled(fixedDelayString = "${procoach.notification.retry.interval-ms:300000}",
			initialDelayString = "${procoach.notification.retry.interval-ms:300000}")
	public void retryFailedDeliveries() {
		try {
			int attempted = dispatcher.retryFailed();
			if (attempted > 0) {
				log.info("Repetidas {} entregas de aviso que haviam falhado", attempted);
			}
		}
		catch (RuntimeException ex) {
			log.error("Falha ao repetir entregas de aviso", ex);
		}
	}
}
