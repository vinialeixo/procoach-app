package com.fitness.procoach.security.interfaces;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fitness.procoach.security.application.AuthService;

/**
 * Resposta do login (T-008): quando o MFA está ativo, vem apenas
 * {@code mfaRequired=true} + {@code challengeId} (token de desafio de 5min);
 * caso contrário, o par de tokens completo.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponse(boolean mfaRequired, String challengeId, String accessToken, String refreshToken,
		String tokenType, Long expiresIn, String role, UUID athleteId) {

	public static LoginResponse from(AuthService.LoginResult result) {
		if (result.mfaRequired()) {
			return new LoginResponse(true, result.challengeId(), null, null, null, null, null, null);
		}
		return new LoginResponse(false, null, result.tokens().accessToken(), result.tokens().refreshToken(),
				result.tokens().tokenType(), result.tokens().expiresIn(), result.tokens().role(),
				result.tokens().athleteId());
	}
}
