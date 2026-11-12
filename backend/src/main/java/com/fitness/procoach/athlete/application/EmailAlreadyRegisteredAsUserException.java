package com.fitness.procoach.athlete.application;

/**
 * T-078: o e-mail informado no cadastro do atleta já pertence a um User.
 * 409 acionável com código {@code EMAIL_ALREADY_REGISTERED} — o coach deve
 * usar outro e-mail (o cadastrado já tem acesso à plataforma).
 */
public class EmailAlreadyRegisteredAsUserException extends RuntimeException {

	/** Código estável para o front mapear a orientação. */
	public static final String CODE = "EMAIL_ALREADY_REGISTERED";

	public EmailAlreadyRegisteredAsUserException(String email) {
		super("O e-mail " + email
				+ " já está cadastrado na plataforma. Use outro e-mail ou convide o atleta pelo e-mail existente.");
	}
}
