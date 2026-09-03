package com.fitness.procoach.assessment.infrastructure;

import com.fitness.procoach.assessment.application.AssessmentApplicationService;
import com.fitness.procoach.assessment.domain.AssessmentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do módulo assessment: expõe o application service como bean Spring.
 */
@Configuration
public class AssessmentConfiguration {

	@Bean
	public AssessmentApplicationService assessmentApplicationService(AssessmentRepository repository) {
		return new AssessmentApplicationService(repository);
	}
}
