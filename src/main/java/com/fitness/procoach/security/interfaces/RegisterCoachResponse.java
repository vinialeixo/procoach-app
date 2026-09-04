package com.fitness.procoach.security.interfaces;

import java.util.UUID;

import com.fitness.procoach.security.application.AuthService;

/** Cadastro de coach: id do usuário criado + sessão já autenticada (auto-login). */
public record RegisterCoachResponse(UUID userId, String accessToken, String refreshToken, String tokenType,
		long expiresIn, String role) {

	public static RegisterCoachResponse from(AuthService.CoachRegistration registration) {
		return new RegisterCoachResponse(registration.userId(), registration.tokens().accessToken(),
				registration.tokens().refreshToken(), registration.tokens().tokenType(),
				registration.tokens().expiresIn(), registration.tokens().role());
	}
}
