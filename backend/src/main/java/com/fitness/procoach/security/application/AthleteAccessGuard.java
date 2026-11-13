package com.fitness.procoach.security.application;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.fitness.procoach.athlete.application.AthleteAccessService;
import com.fitness.procoach.coach.application.CoachApplicationService;
import com.fitness.procoach.coach.application.CoachNotActiveException;

/**
 * Guarda do isolamento de dados por perfil (US-038/T-010), referenciado nas
 * anotações {@code @PreAuthorize} dos controllers via SpEL
 * ({@code @athleteGuard.canAccess(authentication, #athleteId)}).
 *
 * <ul>
 * <li>ROLE_COACH: acesso apenas quando o atleta pertence à carteira do coach
 * autenticado (coachId resolvido server-side a partir do subject do JWT);</li>
 * <li>ROLE_ATHLETE: acesso apenas quando o claim {@code athleteId} do JWT é
 * igual ao atleta solicitado — qualquer outro atleta resulta em 403;</li>
 * <li>demais papéis/principals: negado (fail closed).</li>
 * </ul>
 */
@Component("athleteGuard")
public class AthleteAccessGuard {

	private final CoachApplicationService coachService;
	private final AthleteAccessService athleteAccessService;

	public AthleteAccessGuard(CoachApplicationService coachService, AthleteAccessService athleteAccessService) {
		this.coachService = coachService;
		this.athleteAccessService = athleteAccessService;
	}

	public boolean canAccess(Authentication authentication, UUID athleteId) {
		if (authentication == null || !authentication.isAuthenticated() || athleteId == null) {
			return false;
		}
		if (hasAuthority(authentication, "ROLE_" + AuthService.COACH_ROLE)) {
			if (authentication.getPrincipal() instanceof Jwt jwt) {
				String subject = jwt.getSubject();
				if (subject == null) {
					return false;
				}
				try {
					UUID userId = UUID.fromString(subject);
					var coachId = coachService.requireCoachIdByUserId(userId);
					return athleteAccessService.belongsToCoach(athleteId, coachId.value());
				} catch (CoachNotActiveException | IllegalArgumentException e) {
					return false;
				}
			}
			return false;
		}
		if (!hasAuthority(authentication, "ROLE_" + AuthService.ATHLETE_ROLE)) {
			return false;
		}
		if (authentication.getPrincipal() instanceof Jwt jwt) {
			String claim = jwt.getClaimAsString("athleteId");
			return claim != null && claim.equals(athleteId.toString());
		}
		return false;
	}

	private boolean hasAuthority(Authentication authentication, String authority) {
		return authentication.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
	}
}
