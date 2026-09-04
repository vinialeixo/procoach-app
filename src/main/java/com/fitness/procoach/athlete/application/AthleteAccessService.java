package com.fitness.procoach.athlete.application;

import com.fitness.procoach.athlete.domain.Athlete;
import com.fitness.procoach.athlete.domain.AthleteId;
import com.fitness.procoach.athlete.domain.AthleteRepository;
import java.util.UUID;

/**
 * Fachada pública do módulo athlete (T-058) para checagem de posse. É o único
 * ponto pelo qual os demais módulos (assessment/nutrition/training) verificam
 * se um atleta pertence ao coach autenticado — respeitando o boundary do
 * Modulith: nunca acessam {@code Athlete}/{@code AthleteRepository} direto.
 *
 * <p>Framework-agnostic: registrado como bean pela configuração da camada de
 * infraestrutura.</p>
 */
public class AthleteAccessService {

	private final AthleteRepository repository;

	public AthleteAccessService(AthleteRepository repository) {
		this.repository = repository;
	}

	/**
	 * {@code true} se existe um atleta não removido (soft-delete) com o id dado
	 * pertencente ao coach dado.
	 */
	public boolean belongsToCoach(UUID athleteId, UUID coachId) {
		return repository.findById(AthleteId.of(athleteId))
				.filter(athlete -> !athlete.isDeleted())
				.map(athlete -> athlete.coachId().equals(coachId))
				.orElse(false);
	}

	/**
	 * Carrega o atleta e garante que pertence ao coach dado. Lança
	 * {@link AthleteNotFoundException} (404) se o atleta não existe, foi
	 * removido (soft-delete) ou pertence a outro coach — não vaza a existência
	 * do recurso.
	 */
	public Athlete requireOwnedByCoach(UUID athleteId, UUID coachId) {
		Athlete athlete = repository.findById(AthleteId.of(athleteId))
				.filter(a -> !a.isDeleted())
				.orElseThrow(() -> new AthleteNotFoundException(athleteId));
		if (!athlete.coachId().equals(coachId)) {
			throw new AthleteNotFoundException(athleteId);
		}
		return athlete;
	}
}
