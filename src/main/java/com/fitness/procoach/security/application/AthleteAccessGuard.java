package com.fitness.procoach.security.application;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Guarda do isolamento de dados por perfil (US-038/T-010), referenciado nas
 * anotações {@code @PreAuthorize} dos controllers via SpEL
 * ({@code @athleteGuard.canAccess(authentication, #athleteId)}).
 *
 * <ul>
 * <li>ROLE_COACH: acesso liberado à carteira de atletas;</li>
 * <li>ROLE_ATHLETE: acesso apenas quando o claim {@code athleteId} do JWT é
 * igual ao atleta solicitado — qualquer outro atleta resulta em 403;</li>
 * <li>demais papéis/principals: negado (fail closed).</li>
 * </ul>
 */
@Component("athleteGuard")
public class AthleteAccessGuard {

	public boolean canAccess(Authentication authentication, UUID athleteId) {
		if (authentication == null || !authentication.isAuthenticated() || athleteId == null) {
			return false;
		}
		if (hasAuthority(authentication, "ROLE_" + AuthService.COACH_ROLE)) {
			return true;
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
