package com.fitness.procoach.nutrition.infrastructure;

import com.fitness.procoach.nutrition.application.DietApplicationService;
import com.fitness.procoach.nutrition.domain.DietRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do módulo nutrition: expõe o application service como bean Spring.
 */
@Configuration
public class NutritionConfiguration {

	@Bean
	public DietApplicationService dietApplicationService(DietRepository repository) {
		return new DietApplicationService(repository);
	}
}
