package com.fitness.procoach.athlete.interfaces;

import com.fitness.procoach.athlete.domain.Goal;
import com.fitness.procoach.athlete.domain.Sex;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de entrada do cadastro de atleta (T-018/US-001). Todos os campos são
 * obrigatórios e validados antes de chegar ao domínio. O coachId NÃO faz parte
 * do request (T-058): é resolvido server-side a partir do usuário autenticado.
 */
@Schema(description = "Dados para cadastro de um novo atleta.")
public record RegisterAthleteRequest(
		@Schema(description = "Nome completo do atleta", example = "Ana Silva")
		@NotBlank(message = "name é obrigatório") @Size(max = 150, message = "name deve ter no máximo 150 caracteres") String name,
		@Schema(description = "Sexo biológico do atleta", example = "FEMALE")
		@NotNull(message = "sex é obrigatório") Sex sex,
		@Schema(description = "Data de nascimento (não pode ser futura)", example = "1995-04-12")
		@NotNull(message = "birthDate é obrigatório") @PastOrPresent(message = "birthDate não pode ser futura") LocalDate birthDate,
		@Schema(description = "Altura em centímetros", example = "165.0")
		@NotNull(message = "height é obrigatório") @Positive(message = "height deve ser positivo") BigDecimal height,
		@Schema(description = "Peso em quilogramas", example = "62.5")
		@NotNull(message = "weight é obrigatório") @Positive(message = "weight deve ser positivo") BigDecimal weight,
		@Schema(description = "Objetivo do acompanhamento", example = "HYPERTROPHY")
		@NotNull(message = "goal é obrigatório") Goal goal,
		@Schema(description = "E-mail do atleta (opcional; usado para dedupe de credencial, T-078)", example = "atleta@test.com") @jakarta.validation.constraints.Email(message = "email inválido") String email) {
}
