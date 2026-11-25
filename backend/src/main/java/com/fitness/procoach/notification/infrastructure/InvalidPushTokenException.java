package com.fitness.procoach.notification.infrastructure;

/**
 * O provedor de push recusou o token do dispositivo de forma definitiva (ex.: app desinstalado ou
 * token expirado). O canal remove o registro em vez de repetir a entrega.
 */
public class InvalidPushTokenException extends RuntimeException {

	private final String deviceToken;

	public InvalidPushTokenException(String deviceToken, String detail) {
		super("token de push inválido: " + detail);
		this.deviceToken = deviceToken;
	}

	public String getDeviceToken() {
		return deviceToken;
	}
}
