package com.fitness.procoach.athlete.interfaces;

import com.fitness.procoach.athlete.application.AthleteAccessInfoProvider;
import com.fitness.procoach.athlete.domain.Athlete;
import com.fitness.procoach.athlete.domain.AthleteStatus;
import com.fitness.procoach.athlete.domain.Goal;
import com.fitness.procoach.athlete.domain.Sex;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO de saída de um atleta. O {@code id} único e imutável (US-002) é exposto
 * na ficha do atleta. Desde a T-077 também traz o estado de acesso
 * ({@code hasCredential} e {@code pendingInvite}) para os badges do front
 * ("Tem acesso / Convite pendente / Sem acesso").
 */
@Schema(description = "Ficha de um atleta cadastrado. O id é único e imutável (US-002).")
public record AthleteResponse(
		@Schema(description = "Identificador único e imutável do atleta (US-002)",
				example = "8c1d2e3f-4a5b-4c6d-8e9f-0a1b2c3d4e5f") UUID id,
		@Schema(description = "Identificador do coach responsável",
				example = "2f6b1c3d-7a0e-4b5d-9c8a-1e2f3a4b5c6d") UUID coachId,
		@Schema(description = "Nome completo do atleta", example = "Ana Silva") String name,
		@Schema(description = "Sexo biológico do atleta", example = "FEMALE") Sex sex,
		@Schema(description = "Data de nascimento", example = "1995-04-12") LocalDate birthDate,
		@Schema(description = "Altura em centímetros", example = "165.0") BigDecimal height,
		@Schema(description = "Peso em quilogramas", example = "62.5") BigDecimal weight,
		@Schema(description = "Objetivo do acompanhamento", example = "HYPERTROPHY") Goal goal,
		@Schema(description = "Estado do acompanhamento", example = "ACTIVE") AthleteStatus status,
		@Schema(description = "Data/hora de criação do registro", example = "2026-09-01T10:15:30Z") Instant createdAt,
		@Schema(description = "Data/hora da última atualização", example = "2026-09-01T10:15:30Z") Instant updatedAt,
		@Schema(description = "O atleta já criou a própria credencial de acesso (T-077)") boolean hasCredential,
		@Schema(description = "Convite pendente (e-mail e expiração), ou null (T-077)") PendingInviteResponse pendingInvite) {

	@Schema(description = "Convite de acesso pendente. O token nunca é devolvido aqui.")
	public record PendingInviteResponse(
			@Schema(description = "Id do convite") UUID id,
			@Schema(description = "E-mail do convite", example = "atleta@test.com") String email,
			@Schema(description = "Expira em", example = "2026-11-19T10:15:30Z") Instant expiresAt) {
	}

	public static AthleteResponse from(Athlete athlete) {
		return from(athlete, false, null);
	}

	/** Versão com o estado de acesso preenchido (T-077); pendência nula quando não há convite. */
	public static AthleteResponse from(Athlete athlete, boolean hasCredential,
			AthleteAccessInfoProvider.PendingInvite pendingInvite) {
		return new AthleteResponse(athlete.id().value(), athlete.coachId(), athlete.name().value(),
				athlete.sex(), athlete.birthDate(), athlete.height().value(), athlete.weight().value(),
				athlete.goal(), athlete.status(), athlete.createdAt(), athlete.updatedAt(),
				hasCredential,
				pendingInvite == null ? null
						: new PendingInviteResponse(pendingInvite.id(), pendingInvite.email(),
								pendingInvite.expiresAt()));
	}
}
