package com.fitness.procoach.security.infrastructure;

import com.fitness.procoach.athlete.application.AthleteAccessInfoProvider;
import com.fitness.procoach.audit.application.AuditData;
import com.fitness.procoach.audit.application.AuditService;
import com.fitness.procoach.security.application.AthleteInviteService;
import com.fitness.procoach.security.domain.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Implementação da porta de estado de acesso do atleta (T-077/T-078/issue #140)
 * — o módulo athlete consulta/revoga acesso sem depender de security (sem ciclo
 * no Modulith).
 */
@Component
public class UserAthleteAccessInfoProvider implements AthleteAccessInfoProvider {

	private final UserRepository users;
	private final AthleteInviteService inviteService;
	private final AuditService auditService;

	public UserAthleteAccessInfoProvider(UserRepository users, AthleteInviteService inviteService,
			AuditService auditService) {
		this.users = users;
		this.inviteService = inviteService;
		this.auditService = auditService;
	}

	@Override
	public boolean hasCredential(UUID athleteId) {
		return users.existsByAthleteId(athleteId);
	}

	@Override
	public Optional<PendingInvite> pendingInvite(UUID athleteId) {
		return inviteService.findPendingByAthleteId(athleteId)
				.map(invite -> new PendingInvite(invite.getId(), invite.getEmail(), invite.getExpiresAt()));
	}

	@Override
	public boolean emailBelongsToUser(String email) {
		if (email == null || email.isBlank()) {
			return false;
		}
		// T-096: dedupe de atleta é case-insensitive, pois e-mails são
		// case-insensitive por convenção. Normaliza (trim) antes de comparar.
		return users.existsByEmailIgnoreCase(email.trim());
	}

	@Override
	public void revokeAccess(UUID athleteId, UUID actorUserId) {
		users.findByAthleteId(athleteId).ifPresent(user -> {
			if (user.isEnabled()) {
				user.disable();
				users.save(user);
				auditService.record("User", user.getId(), athleteId, "ATHLETE_ACCESS_REVOKED", actorUserId,
						AuditData.of());
			}
		});
		inviteService.revokeAllOpenByAthlete(athleteId, actorUserId);
	}

	@Override
	public void restoreAccess(UUID athleteId) {
		users.findByAthleteId(athleteId).ifPresent(user -> {
			if (!user.isEnabled()) {
				user.enable();
				users.save(user);
			}
		});
	}
}
