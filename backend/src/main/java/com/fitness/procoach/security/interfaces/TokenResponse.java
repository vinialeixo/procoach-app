package com.fitness.procoach.security.interfaces;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fitness.procoach.security.application.AuthTokens;

/**
 * Par de tokens de uma sessão autenticada. {@code athleteId} só aparece para
 * credenciais de atleta (claim do isolamento da T-010).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn, String role,
		UUID athleteId) {

	public static TokenResponse from(AuthTokens tokens) {
		return new TokenResponse(tokens.accessToken(), tokens.refreshToken(), tokens.tokenType(), tokens.expiresIn(),
				tokens.role(), tokens.athleteId());
	}
}
