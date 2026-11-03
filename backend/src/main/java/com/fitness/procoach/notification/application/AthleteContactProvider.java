package com.fitness.procoach.notification.application;

import java.util.Optional;
import java.util.UUID;

/**
 * Porta para descobrir como falar com o atleta fora do app. Implementada pelo módulo que conhece
 * as credenciais (security), para o módulo de avisos não depender dele.
 */
public interface AthleteContactProvider {

	/** E-mail de acesso do atleta, se ele já criou a conta. */
	Optional<String> emailOf(UUID athleteId);
}
