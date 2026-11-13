package com.fitness.procoach.security.application;

import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.fitness.procoach.athlete.application.AthleteAccessService;
import com.fitness.procoach.athlete.application.AthleteNotFoundException;
import com.fitness.procoach.coach.application.CoachApplicationService;
import com.fitness.procoach.coach.application.CoachNotActiveException;

/**
 * Guarda cross-cutting de posse coach↔atleta (anti-IDOR, T-058).
 * Resolve o {@code coachId} a partir do usuário autenticado (subject do JWT —
 * nunca do request) e garante que o atleta pertence à carteira desse coach.
 *
 * <p>Recurso inexistente OU de atleta de outro coach resulta no mesmo
 * {@code 404 Not Found} (não vaza a existência do recurso).</p>
 *
 * <p>Uso típico em controllers:</p>
 * <pre>
 * {@code
 * @Autowired private CoachAthleteAccessGuard coachAthleteGuard;
 *
 * private void requireCoachOwnsAthlete(Jwt jwt, UUID athleteId) {
 *     coachAthleteGuard.requireCoachOwnsAthlete(jwt, athleteId);
 * }}
 * </pre>
 */
@Component
public class CoachAthleteAccessGuard {

    private final CoachApplicationService coachService;
    private final AthleteAccessService athleteAccessService;

    public CoachAthleteAccessGuard(CoachApplicationService coachService, AthleteAccessService athleteAccessService) {
        this.coachService = coachService;
        this.athleteAccessService = athleteAccessService;
    }

    /**
     * Garante que o atleta ({@code athleteId}) pertence à carteira do coach
     * vinculado ao usuário autenticado ({@code jwt.getSubject()}).
     *
     * @param jwt        JWT do usuário autenticado (deve conter subject e role "COACH")
     * @param athleteId  Identificador do atleta a ser validado
     * @throws AthleteNotFoundException se o atleta não existe, foi removido (soft-delete)
     *                                  ou pertence a outro coach — mesmo status 404 para não vazar existência
     * @throws CoachNotActiveException  se o usuário autenticado não tem Coach vinculado ou o Coach não está ACTIVE
     * @throws IllegalArgumentException se o JWT não contém subject válido
     */
    public void requireCoachOwnsAthlete(Jwt jwt, UUID athleteId) {
        if (jwt == null || !"COACH".equals(jwt.getClaimAsString("role"))) {
            return; // Chamada de atleta (self-access) já validada pelo @athleteGuard
        }
        UUID userId = UUID.fromString(jwt.getSubject());
        UUID coachId = coachService.requireCoachIdByUserId(userId).value();
        athleteAccessService.requireOwnedByCoach(athleteId, coachId);
    }

    /**
     * Verifica se o JWT representa uma chamada de atleta (self-access).
     *
     * @param jwt JWT do usuário autenticado
     * @return {@code true} se o JWT tem role "ATHLETE", {@code false} caso contrário
     */
    public static boolean isAthlete(Jwt jwt) {
        return jwt != null && "ATHLETE".equals(jwt.getClaimAsString("role"));
    }

    /**
     * Verifica se o JWT representa uma chamada de coach.
     *
     * @param jwt JWT do usuário autenticado
     * @return {@code true} se o JWT tem role "COACH", {@code false} caso contrário
     */
    public static boolean isCoach(Jwt jwt) {
        return jwt != null && "COACH".equals(jwt.getClaimAsString("role"));
    }
}