package com.fitness.procoach.notification.domain;

public enum DeliveryStatus {
	/** Ainda sem tentativa. */
	PENDING,
	/** Entregue ao canal (e-mail enviado, push aceito pelo provedor). */
	SENT,
	/** Última tentativa falhou; pode ser repetida até o limite de tentativas. */
	FAILED,
	/** Sem destino (atleta sem e-mail ou sem dispositivo): nada a repetir. */
	SKIPPED
}
