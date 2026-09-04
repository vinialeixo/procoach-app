package com.fitness.procoach.coach.application;

import java.util.UUID;

/**
 * Coach não encontrado ou não ativo para o usuário autenticado (subject do
 * JWT). Lançada pela fachada {@link CoachApplicationService} quando o usuário
 * não possui um Coach ACTIVE vinculado. Mapeada para {@code 403 Forbidden} —
 * o usuário está autenticado, mas não tem permissão de coach.
 */
public class CoachNotActiveException extends RuntimeException {

	public CoachNotActiveException(UUID userId) {
		super("No active coach linked to user: " + userId);
	}
}
