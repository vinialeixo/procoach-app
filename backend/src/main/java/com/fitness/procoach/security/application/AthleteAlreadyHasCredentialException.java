package com.fitness.procoach.security.application;

/** O atleta já tem credencial de acesso. */
public class AthleteAlreadyHasCredentialException extends RuntimeException {

	public AthleteAlreadyHasCredentialException() {
		super("Este atleta já possui acesso");
	}
}
