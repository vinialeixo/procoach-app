package com.fitness.procoach.security.interfaces;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MfaVerifyRequest(
		@NotBlank String challengeId,
		@NotBlank @Pattern(regexp = "\\d{6}", message = "O código tem 6 dígitos numéricos") String code) {
}
