package com.fitness.procoach.security.infrastructure;

import com.fitness.procoach.athlete.application.AthleteAccessInfoProvider;
import com.fitness.procoach.security.application.AthleteInviteService;
import com.fitness.procoach.security.domain.AthleteInvite;
import com.fitness.procoach.security.domain.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Implementação da porta de estado de acesso do atleta (T-077/T-078) — o
 * módulo athlete consulta sem depender de security (sem ciclo no Modulith).
 */
@Component
public class UserAthleteAccessInfoProvider implements AthleteAccessInfoProvider {

	private final UserRepository users;
	private final AthleteInviteService inviteService;

	public UserAthleteAccessInfoProvider(UserRepository users, AthleteInviteService inviteService) {
		this.users = users;
		this.inviteService = inviteService;
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
		return users.findByEmail(email).isPresent();
	}
}
