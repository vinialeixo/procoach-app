package com.fitness.procoach.athlete.application;

import com.fitness.procoach.athlete.domain.Athlete;
import com.fitness.procoach.athlete.domain.AthleteId;
import com.fitness.procoach.athlete.domain.AthleteRepository;
import com.fitness.procoach.athlete.domain.Goal;
import com.fitness.procoach.athlete.domain.Height;
import com.fitness.procoach.athlete.domain.Name;
import com.fitness.procoach.athlete.domain.Sex;
import com.fitness.procoach.athlete.domain.Weight;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Application service do módulo athlete (T-018). Orquestra o domínio e a porta
 * de persistência. Framework-agnostic: registrado como bean pela configuração
 * da camada de infraestrutura.
 */
public class AthleteApplicationService {

	private final AthleteRepository repository;

	public AthleteApplicationService(AthleteRepository repository) {
		this.repository = repository;
	}

	/**
	 * Cadastra um novo atleta (US-001). O {@link AthleteId} único e imutável é
	 * gerado aqui (US-002); o status inicial é {@code ACTIVE}. As regras de
	 * domínio (nome não vazio, data de nascimento não futura, altura/peso
	 * positivos) são enforceadas pelos value objects e pelo agregado.
	 */
	public Athlete register(RegisterAthleteCommand command) {
		Athlete athlete = Athlete.register(
				AthleteId.generate(),
				command.coachId(),
				new Name(command.name()),
				command.sex(),
				command.birthDate(),
				new Height(command.height()),
				new Weight(command.weight()),
				command.goal());
		return repository.save(athlete);
	}

	public record RegisterAthleteCommand(UUID coachId, String name, Sex sex, LocalDate birthDate,
			BigDecimal height, BigDecimal weight, Goal goal) {
	}
}
