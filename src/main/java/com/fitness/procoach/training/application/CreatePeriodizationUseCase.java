package com.fitness.procoach.training.application;

import java.time.LocalDate;
import java.util.UUID;

import com.fitness.procoach.athlete.application.AthleteAccessService;
import com.fitness.procoach.coach.application.CoachApplicationService;
import com.fitness.procoach.training.domain.model.Periodization;
import com.fitness.procoach.training.domain.repository.PeriodizationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreatePeriodizationUseCase {

	private final PeriodizationRepository periodizationRepository;

	private final CoachApplicationService coachService;

	private final AthleteAccessService athleteAccessService;

	public CreatePeriodizationUseCase(PeriodizationRepository periodizationRepository,
			CoachApplicationService coachService, AthleteAccessService athleteAccessService) {
		this.periodizationRepository = periodizationRepository;
		this.coachService = coachService;
		this.athleteAccessService = athleteAccessService;
	}

	public Periodization execute(UUID athleteId, UUID userId, String name, String description, LocalDate startDate,
			LocalDate endDate) {
		// T-058: o coachId nunca vem do request — é resolvido server-side a partir
		// do usuário autenticado, e o atleta-alvo precisa pertencer a esse coach.
		UUID coachId = coachService.requireCoachIdByUserId(userId).value();
		athleteAccessService.requireOwnedByCoach(athleteId, coachId);
		Periodization periodization = Periodization.create(athleteId, coachId, name, description, startDate, endDate);
		return this.periodizationRepository.save(periodization);
	}

}
