package com.fitness.procoach.security.application;

/** E-mail já cadastrado (HTTP 409). */
public class EmailAlreadyRegisteredException extends RuntimeException {

	public EmailAlreadyRegisteredException(String email) {
		super("E-mail já cadastrado: " + email);
	}
}
