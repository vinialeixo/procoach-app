package com.fitness.procoach.security.application;

/** Credenciais inválidas ou usuário desabilitado (HTTP 401). */
public class InvalidCredentialsException extends RuntimeException {

	public InvalidCredentialsException() {
		super("Credenciais inválidas");
	}
}
