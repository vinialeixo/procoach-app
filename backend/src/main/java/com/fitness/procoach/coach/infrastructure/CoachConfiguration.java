package com.fitness.procoach.coach.infrastructure;

import com.fitness.procoach.coach.application.CoachApplicationService;
import com.fitness.procoach.coach.domain.CoachRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do módulo coach: expõe a fachada de aplicação como bean Spring.
 */
@Configuration
public class CoachConfiguration {

	@Bean
	public CoachApplicationService coachApplicationService(CoachRepository repository) {
		return new CoachApplicationService(repository);
	}
}
