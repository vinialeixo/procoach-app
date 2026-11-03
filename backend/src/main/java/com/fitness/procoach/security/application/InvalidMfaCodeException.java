package com.fitness.procoach.security.application;

/** Código TOTP de 6 dígitos inválido (HTTP 400). */
public class InvalidMfaCodeException extends RuntimeException {

	public InvalidMfaCodeException() {
		super("Código de verificação inválido ou expirado");
	}
}
