package com.fitness.procoach.security.application;

/** Refresh token ou desafio de MFA inválido, expirado ou de tipo errado (HTTP 401). */
public class InvalidTokenException extends RuntimeException {

	public InvalidTokenException(String message) {
		super(message);
	}
}
