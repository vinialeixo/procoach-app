package com.fitness.procoach.notification.application;

import com.fitness.procoach.notification.domain.Notification;
import com.fitness.procoach.notification.domain.NotificationChannelType;

/**
 * Canal externo de entrega de um aviso (e-mail, push). A implementação decide se está disponível
 * (configurada) e levanta exceção em caso de falha — o despachante registra e repete depois.
 */
public interface NotificationChannel {

	/** Resultado de uma tentativa que não falhou. */
	record Result(boolean skipped, String detail) {

		public static Result sent() {
			return new Result(false, null);
		}

		/** Sem destino (ex.: atleta sem e-mail ou sem dispositivo): nada a repetir. */
		public static Result skipped(String reason) {
			return new Result(true, reason);
		}
	}

	NotificationChannelType type();

	/** {@code false} quando o canal não está configurado neste ambiente; ele é ignorado. */
	boolean available();

	Result send(Notification notification) throws Exception;
}
