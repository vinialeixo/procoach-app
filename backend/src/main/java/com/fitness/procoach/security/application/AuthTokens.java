package com.fitness.procoach.security.application;

import java.util.UUID;

/**
 * Par de tokens emitido após autenticação completa (senha + MFA, quando ativo).
 * {@code role} é o nome do papel sem prefixo ("COACH"/"ATHLETE") e
 * {@code athleteId} só é preenchido para credenciais de atleta — o claim que
 * ancora o isolamento de dados da T-010.
 */
public record AuthTokens(String accessToken, String refreshToken, String tokenType, long expiresIn, String role,
		UUID athleteId) {
}
