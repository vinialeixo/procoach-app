package com.fitness.procoach.notification.infrastructure;

/**
 * Provedor de push (FCM, APNs, Web Push). A integração real entra como uma implementação desta
 * interface, selecionada por {@code procoach.notification.push.provider}; sem provedor, o canal
 * de push fica indisponível.
 */
public interface PushGateway {

	/** Envia a notificação para um dispositivo; levanta exceção se o provedor recusar. */
	void send(String deviceToken, String title, String body);
}
