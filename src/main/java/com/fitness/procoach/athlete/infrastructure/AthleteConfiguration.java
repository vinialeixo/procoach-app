package com.fitness.procoach.athlete.infrastructure;

import com.fitness.procoach.athlete.application.AthleteAccessService;
import com.fitness.procoach.athlete.application.AthleteApplicationService;
import com.fitness.procoach.athlete.domain.AthleteRepository;
import com.fitness.procoach.coach.application.CoachApplicationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do módulo athlete: expõe os application services como beans Spring.
 */
@Configuration
public class AthleteConfiguration {

	@Bean
	public AthleteApplicationService athleteApplicationService(AthleteRepository repository,
			CoachApplicationService coachService) {
		return new AthleteApplicationService(repository, coachService);
	}

	@Bean
	public AthleteAccessService athleteAccessService(AthleteRepository repository) {
		return new AthleteAccessService(repository);
	}
}
