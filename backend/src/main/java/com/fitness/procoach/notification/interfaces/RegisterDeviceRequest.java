package com.fitness.procoach.notification.interfaces;

import com.fitness.procoach.notification.domain.DevicePlatform;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Dispositivo do atleta para receber push.")
public record RegisterDeviceRequest(
		@Schema(description = "Token do dispositivo emitido pelo provedor de push") @NotBlank(message = "token é obrigatório")
		@Size(max = 512, message = "token deve ter no máximo 512 caracteres") String token,
		@Schema(description = "Plataforma do dispositivo", example = "ANDROID")
		@NotNull(message = "platform é obrigatório") DevicePlatform platform) {
}
