package com.fitness.procoach.security.application;

/**
 * Segredo TOTP gerado no primeiro passo da ativação do MFA. {@code otpauthUrl}
 * é o conteúdo a ser renderizado como QR Code pelo cliente.
 */
public record MfaSetupResult(String secret, String otpauthUrl) {
}
