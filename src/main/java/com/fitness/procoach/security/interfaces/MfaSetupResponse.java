package com.fitness.procoach.security.interfaces;

import com.fitness.procoach.security.application.MfaSetupResult;

/** Segredo TOTP + otpauth:// URL (conteúdo do QR Code exibido no front). */
public record MfaSetupResponse(String secret, String otpauthUrl) {

	public static MfaSetupResponse from(MfaSetupResult result) {
		return new MfaSetupResponse(result.secret(), result.otpauthUrl());
	}
}
