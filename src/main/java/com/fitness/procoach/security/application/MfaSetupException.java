package com.fitness.procoach.security.application;

/** Violação do fluxo de ativação do MFA — ex.: setup quando o MFA já está ativo (HTTP 409). */
public class MfaSetupException extends RuntimeException {

	public MfaSetupException(String message) {
		super(message);
	}
}
