package com.fitness.procoach.library.infrastructure;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fitness.procoach.library.application.ExerciseApplicationService;
import com.fitness.procoach.library.domain.ExerciseRepository;

/** Configuração do módulo library: expõe o application service como bean Spring. */
@Configuration
public class LibraryConfiguration {

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	public ExerciseApplicationService exerciseApplicationService(ExerciseRepository repository, Clock clock) {
		return new ExerciseApplicationService(repository, clock);
	}
}
