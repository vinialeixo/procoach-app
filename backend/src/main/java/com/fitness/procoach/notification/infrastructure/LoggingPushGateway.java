package com.fitness.procoach.notification.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Provedor de push de desenvolvimento ({@code procoach.notification.push.provider=log}): só
 * registra no log, sem enviar nada. Serve para exercitar o fluxo sem credencial de provedor.
 */
@Component
@ConditionalOnProperty(name = "procoach.notification.push.provider", havingValue = "log")
public class LoggingPushGateway implements PushGateway {

	private static final Logger log = LoggerFactory.getLogger(LoggingPushGateway.class);

	@Override
	public void send(String deviceToken, String title, String body) {
		String shortToken = deviceToken.length() > 8 ? deviceToken.substring(0, 8) + "…" : deviceToken;
		log.info("[push simulado] dispositivo {}: {} — {}", shortToken, title, body);
	}
}
