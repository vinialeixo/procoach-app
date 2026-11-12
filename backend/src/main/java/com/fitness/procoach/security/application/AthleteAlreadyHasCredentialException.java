package com.fitness.procoach.security.application;

/** O atleta já tem credencial de acesso. T-079: código estável para o front. */
public class AthleteAlreadyHasCredentialException extends RuntimeException {

	/** Código estável do 409 (T-079). */
	public static final String CODE = "ATHLETE_HAS_CREDENTIAL";

	public AthleteAlreadyHasCredentialException() {
		super("Este atleta já possui acesso. Recupere a senha existente em vez de convidar novamente.");
	}
}
