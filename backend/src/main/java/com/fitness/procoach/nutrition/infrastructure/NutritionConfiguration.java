package com.fitness.procoach.nutrition.infrastructure;

import com.fitness.procoach.athlete.application.AthleteAccessService;
import com.fitness.procoach.assessment.application.AssessmentApplicationService;
import com.fitness.procoach.audit.application.AuditService;
import com.fitness.procoach.notification.application.NotificationService;
import com.fitness.procoach.coach.application.CoachApplicationService;
import com.fitness.procoach.nutrition.application.DietApplicationService;
import com.fitness.procoach.nutrition.application.FoodApplicationService;
import com.fitness.procoach.nutrition.domain.DietRepository;
import com.fitness.procoach.nutrition.domain.FoodRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do módulo nutrition: expõe o application service como bean Spring.
 */
@Configuration
public class NutritionConfiguration {

	@Bean
	public DietApplicationService dietApplicationService(DietRepository repository,
			CoachApplicationService coachService, AthleteAccessService athleteAccessService,
			AssessmentApplicationService assessmentService, AuditService auditService,
			NotificationService notificationService) {
		return new DietApplicationService(repository, coachService, athleteAccessService, assessmentService,
				auditService, notificationService);
	}

	@Bean
	public FoodApplicationService foodApplicationService(FoodRepository repository,
			CoachApplicationService coachService, AuditService auditService) {
		return new FoodApplicationService(repository, coachService, auditService);
	}
}
