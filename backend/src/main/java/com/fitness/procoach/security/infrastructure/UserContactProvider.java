package com.fitness.procoach.security.infrastructure;

import com.fitness.procoach.notification.application.AthleteContactProvider;
import com.fitness.procoach.security.domain.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Entrega ao módulo de avisos o e-mail de acesso do atleta (o mesmo do convite), sem que o módulo
 * de avisos dependa de security.
 */
@Component
public class UserContactProvider implements AthleteContactProvider {

	private final UserRepository users;

	public UserContactProvider(UserRepository users) {
		this.users = users;
	}

	@Override
	public Optional<String> emailOf(UUID athleteId) {
		return users.findByAthleteId(athleteId).filter(user -> user.isEnabled()).map(user -> user.getEmail());
	}
}
