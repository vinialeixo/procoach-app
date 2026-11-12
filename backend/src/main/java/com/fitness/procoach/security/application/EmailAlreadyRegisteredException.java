package com.fitness.procoach.security.application;

/**
 * E-mail já cadastrado (HTTP 409). T-079: no convite, o código é
 * {@code EMAIL_IN_USE} — distinto de {@code ATHLETE_HAS_CREDENTIAL} para o
 * front orientar o coach (o e-mail pertence a outro usuário da plataforma).
 */
public class EmailAlreadyRegisteredException extends RuntimeException {

	/** Código estável do 409 no fluxo de convite (T-079). */
	public static final String CODE = "EMAIL_IN_USE";

	public EmailAlreadyRegisteredException(String email) {
		super("O e-mail " + email + " já está registrado por outro usuário. Use outro e-mail para o convite.");
	}
}
