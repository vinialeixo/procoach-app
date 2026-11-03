package com.fitness.procoach.training.application;

import java.util.UUID;

import com.fitness.procoach.athlete.application.AthleteAccessService;
import com.fitness.procoach.coach.application.CoachApplicationService;

import org.springframework.stereotype.Service;

/**
 * Guarda de posse dos recursos do módulo training (T-058, anti-IDOR). Resolve
 * o coachId a partir do usuário autenticado (subject do JWT — nunca do
 * request) e garante que o atleta dono do recurso pertence à carteira desse
 * coach, via {@link CoachApplicationService} e {@link AthleteAccessService} —
 * respeitando os boundaries do Modulith.
 *
 * <p>Recurso inexistente OU de atleta de outro coach resulta no mesmo
 * {@code 404 Not Found} (não vaza a existência do recurso).</p>
 */
@Service
public class TrainingOwnershipGuard {

	private final CoachApplicationService coachService;

	private final AthleteAccessService athleteAccessService;

	public TrainingOwnershipGuard(CoachApplicationService coachService, AthleteAccessService athleteAccessService) {
		this.coachService = coachService;
		this.athleteAccessService = athleteAccessService;
	}

	/**
	 * Garante que o atleta do recurso ({@code athleteId}) pertence à carteira
	 * do coach vinculado ao usuário autenticado ({@code userId} = subject do
	 * JWT). Lança {@link com.fitness.procoach.athlete.application.AthleteNotFoundException}
	 * (404) quando o atleta não existe, foi removido ou é de outro coach.
	 */
	public void requireResourceOwnedByCoach(UUID athleteId, UUID userId) {
		UUID coachId = coachService.requireCoachIdByUserId(userId).value();
		athleteAccessService.requireOwnedByCoach(athleteId, coachId);
	}
}
