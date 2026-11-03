package com.fitness.procoach.security.application;

/**
 * Convite inexistente, expirado, revogado ou já usado. Mensagem única e genérica
 * de propósito: não revela qual dessas situações ocorreu.
 */
public class InvalidInviteException extends RuntimeException {

	public InvalidInviteException() {
		super("Convite inválido ou expirado");
	}
}
