package com.fitness.procoach.athlete.interfaces;

import com.fitness.procoach.athlete.domain.Goal;
import com.fitness.procoach.athlete.domain.Sex;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO de entrada do cadastro de atleta (T-018/US-001). Todos os campos são
 * obrigatórios e validados antes de chegar ao domínio.
 */
public record RegisterAthleteRequest(
		@NotNull(message = "coachId é obrigatório") UUID coachId,
		@NotBlank(message = "name é obrigatório") @Size(max = 150, message = "name deve ter no máximo 150 caracteres") String name,
		@NotNull(message = "sex é obrigatório") Sex sex,
		@NotNull(message = "birthDate é obrigatório") @PastOrPresent(message = "birthDate não pode ser futura") LocalDate birthDate,
		@NotNull(message = "height é obrigatório") @Positive(message = "height deve ser positivo") BigDecimal height,
		@NotNull(message = "weight é obrigatório") @Positive(message = "weight deve ser positivo") BigDecimal weight,
		@NotNull(message = "goal é obrigatório") Goal goal) {
}
