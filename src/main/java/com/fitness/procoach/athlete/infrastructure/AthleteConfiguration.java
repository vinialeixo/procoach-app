package com.fitness.procoach.athlete.infrastructure;

import com.fitness.procoach.athlete.application.AthleteApplicationService;
import com.fitness.procoach.athlete.domain.AthleteRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do módulo athlete: expõe o application service como bean Spring.
 */
@Configuration
public class AthleteConfiguration {

	@Bean
	public AthleteApplicationService athleteApplicationService(AthleteRepository repository) {
		return new AthleteApplicationService(repository);
	}
}
