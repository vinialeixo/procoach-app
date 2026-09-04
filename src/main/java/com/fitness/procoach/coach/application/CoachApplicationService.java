package com.fitness.procoach.coach.application;

import com.fitness.procoach.coach.domain.Coach;
import com.fitness.procoach.coach.domain.CoachId;
import com.fitness.procoach.coach.domain.CoachRepository;
import com.fitness.procoach.coach.domain.CoachStatus;
import java.util.UUID;

/**
 * Fachada pública do módulo coach (T-058). É o único ponto pelo qual os demais
 * módulos (athlete/assessment/nutrition/training) resolvem o Coach do usuário
 * autenticado — respeitando o boundary do Modulith: nunca acessam o domínio ou
 * a infraestrutura do coach diretamente.
 *
 * <p>Framework-agnostic: registrado como bean pela configuração da camada de
 * infraestrutura.</p>
 */
public class CoachApplicationService {

	private final CoachRepository repository;

	public CoachApplicationService(CoachRepository repository) {
		this.repository = repository;
	}

	/**
	 * Resolve o Coach ACTIVE vinculado ao usuário autenticado (subject do JWT).
	 * Lança {@link CoachNotActiveException} se o usuário não tem Coach ou o
	 * Coach não está ACTIVE. É a âncora da T-058: o coachId nunca vem do
	 * request — é sempre derivado do usuário autenticado.
	 */
	public CoachId requireCoachIdByUserId(UUID userId) {
		Coach coach = repository.findByUserId(userId)
				.orElseThrow(() -> new CoachNotActiveException(userId));
		if (coach.status() != CoachStatus.ACTIVE) {
			throw new CoachNotActiveException(userId);
		}
		return coach.id();
	}

	/**
	 * Resolve o agregado Coach ACTIVE vinculado ao usuário autenticado.
	 * Mesma semântica de {@link #requireCoachIdByUserId}, mas devolve o agregado
	 * para operações que precisam dos invariantes de domínio (ex.: carteira).
	 */
	public Coach requireActiveCoachByUserId(UUID userId) {
		Coach coach = repository.findByUserId(userId)
				.orElseThrow(() -> new CoachNotActiveException(userId));
		if (coach.status() != CoachStatus.ACTIVE) {
			throw new CoachNotActiveException(userId);
		}
		return coach;
	}

	/**
	 * Cria o agregado Coach vinculado ao usuário autenticável (User.id) no
	 * momento do cadastro do coach (T-058). Idempotente: se o usuário já tem um
	 * Coach, devolve o existente sem duplicar.
	 */
	public CoachId registerCoach(UUID userId, String name) {
		if (repository.existsByUserId(userId)) {
			return repository.findByUserId(userId).orElseThrow().id();
		}
		Coach coach = Coach.register(CoachId.generate(), userId, name);
		return repository.save(coach).id();
	}
}
