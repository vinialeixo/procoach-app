package com.fitness.procoach.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import com.fitness.procoach.athlete.application.AthleteAccessService;
import com.fitness.procoach.coach.application.CoachApplicationService;
import com.fitness.procoach.coach.application.CoachNotActiveException;
import com.fitness.procoach.coach.domain.CoachId;
import com.fitness.procoach.security.application.AthleteAccessGuard;
import com.fitness.procoach.security.application.AuthService;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Testes do {@link AthleteAccessGuard} (GitHub #139): validação de posse do
 * atleta pelo coach autenticado — anti-IDOR.
 */
class AthleteAccessGuardTest {

	private AthleteAccessGuard guard;
	private CoachApplicationService coachService;
	private AthleteAccessService athleteAccessService;

	@BeforeEach
	void setUp() {
		coachService = mock(CoachApplicationService.class);
		athleteAccessService = mock(AthleteAccessService.class);
		guard = new AthleteAccessGuard(coachService, athleteAccessService);
	}

	@Test
	void coachCanAccessOwnAthlete() {
		UUID userId = UUID.randomUUID();
		UUID coachId = UUID.randomUUID();
		UUID athleteId = UUID.randomUUID();

		Authentication auth = createCoachAuth(userId);
		when(coachService.requireCoachIdByUserId(userId)).thenReturn(CoachId.of(coachId));
		when(athleteAccessService.belongsToCoach(athleteId, coachId)).thenReturn(true);

		assertThat(guard.canAccess(auth, athleteId)).isTrue();
	}

	@Test
	void coachCannotAccessOtherCoachAthlete() {
		UUID userId = UUID.randomUUID();
		UUID coachId = UUID.randomUUID();
		UUID athleteId = UUID.randomUUID();

		Authentication auth = createCoachAuth(userId);
		when(coachService.requireCoachIdByUserId(userId)).thenReturn(CoachId.of(coachId));
		when(athleteAccessService.belongsToCoach(athleteId, coachId)).thenReturn(false);

		assertThat(guard.canAccess(auth, athleteId)).isFalse();
	}

	@Test
	void coachWithoutActiveCoachProfileCannotAccess() {
		UUID userId = UUID.randomUUID();
		UUID athleteId = UUID.randomUUID();

		Authentication auth = createCoachAuth(userId);
		when(coachService.requireCoachIdByUserId(userId)).thenThrow(new CoachNotActiveException(userId));

		assertThat(guard.canAccess(auth, athleteId)).isFalse();
	}

	@Test
	void coachWithInvalidJwtSubjectCannotAccess() {
		Authentication auth = createCoachAuthWithInvalidSubject();
		UUID athleteId = UUID.randomUUID();

		assertThat(guard.canAccess(auth, athleteId)).isFalse();
	}

	@Test
	void athleteCanAccessOwnData() {
		UUID athleteId = UUID.randomUUID();

		Authentication auth = createAthleteAuth(athleteId);

		assertThat(guard.canAccess(auth, athleteId)).isTrue();
	}

	@Test
	void athleteCannotAccessOtherAthleteData() {
		UUID athleteId = UUID.randomUUID();
		UUID otherAthleteId = UUID.randomUUID();

		Authentication auth = createAthleteAuth(athleteId);

		assertThat(guard.canAccess(auth, otherAthleteId)).isFalse();
	}

	@Test
	void nullAuthenticationReturnsFalse() {
		assertThat(guard.canAccess(null, UUID.randomUUID())).isFalse();
	}

	@Test
	void unauthenticatedAuthenticationReturnsFalse() {
		Authentication auth = new UsernamePasswordAuthenticationToken("user", "pass");
		assertThat(guard.canAccess(auth, UUID.randomUUID())).isFalse();
	}

	@Test
	void nullAthleteIdReturnsFalse() {
		Authentication auth = createCoachAuth(UUID.randomUUID());
		assertThat(guard.canAccess(auth, null)).isFalse();
	}

	@Test
	void unknownRoleReturnsFalse() {
		Authentication auth = new UsernamePasswordAuthenticationToken("user", "pass",
				Set.of(new SimpleGrantedAuthority("ROLE_UNKNOWN")));
		assertThat(guard.canAccess(auth, UUID.randomUUID())).isFalse();
	}

	private Authentication createCoachAuth(UUID userId) {
		Jwt jwt = createJwt(userId.toString(), Map.of("role", "COACH"));
		return new UsernamePasswordAuthenticationToken(jwt, null,
				Set.of(new SimpleGrantedAuthority("ROLE_COACH")));
	}

	private Authentication createCoachAuthWithInvalidSubject() {
		Jwt jwt = createJwt("not-a-uuid", Map.of("role", "COACH"));
		return new UsernamePasswordAuthenticationToken(jwt, null,
				Set.of(new SimpleGrantedAuthority("ROLE_COACH")));
	}

	private Authentication createAthleteAuth(UUID athleteId) {
		Jwt jwt = createJwt(UUID.randomUUID().toString(), Map.of("role", "ATHLETE", "athleteId", athleteId.toString()));
		return new UsernamePasswordAuthenticationToken(jwt, null,
				Set.of(new SimpleGrantedAuthority("ROLE_ATHLETE")));
	}

	private Jwt createJwt(String subject, Map<String, Object> claims) {
		java.util.Map<String, Object> jwtClaims = new java.util.HashMap<>();
		jwtClaims.put("sub", subject);
		jwtClaims.put("type", "access");
		jwtClaims.put("iss", "procoach");
		if (claims.get("role") != null) {
			jwtClaims.put("role", claims.get("role"));
		}
		if (claims.get("athleteId") != null) {
			jwtClaims.put("athleteId", claims.get("athleteId"));
		}
		return new Jwt("token-value", Instant.now(), Instant.now().plusSeconds(3600),
				Map.of("alg", "HS256", "typ", "JWT"),
				jwtClaims);
	}
}