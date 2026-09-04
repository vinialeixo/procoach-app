package com.fitness.procoach.assessment.application;

import com.fitness.procoach.assessment.domain.Assessment;
import com.fitness.procoach.assessment.domain.AssessmentId;
import com.fitness.procoach.assessment.domain.AssessmentRepository;
import com.fitness.procoach.assessment.domain.BodyMeasurements;
import com.fitness.procoach.athlete.application.AthleteAccessService;
import com.fitness.procoach.coach.application.CoachApplicationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Application service do módulo assessment. Orquestra o domínio e a porta de
 * persistência. Framework-agnostic: registrado como bean pela configuração da
 * camada de infraestrutura.
 */
public class AssessmentApplicationService {

	private final AssessmentRepository repository;
	private final CoachApplicationService coachService;
	private final AthleteAccessService athleteAccessService;

	public AssessmentApplicationService(AssessmentRepository repository, CoachApplicationService coachService,
			AthleteAccessService athleteAccessService) {
		this.repository = repository;
		this.coachService = coachService;
		this.athleteAccessService = athleteAccessService;
	}

	/**
	 * Registra uma nova avaliação física (US-004). A data é obrigatória e o
	 * agregado é imutável: cada avaliação é um novo registro, nunca sobrescreve
	 * a anterior. O coachId NUNCA vem do request (T-058): é resolvido
	 * server-side a partir do usuário autenticado, e o atleta-alvo precisa
	 * pertencer a esse coach (senão 404).
	 */
	public Assessment registerAssessment(RegisterAssessmentCommand command) {
		UUID coachId = coachService.requireCoachIdByUserId(command.userId()).value();
		athleteAccessService.requireOwnedByCoach(command.athleteId(), coachId);
		Assessment assessment = Assessment.create(
				command.athleteId(),
				coachId,
				command.assessmentDate(),
				new BodyMeasurements(command.weightKg(), command.heightCm(),
						command.bodyFatPercentage(), command.muscleMassKg(),
						command.waistCm(), command.hipCm()));
		return repository.save(assessment);
	}

	/**
	 * Histórico de avaliações de um atleta em ordem cronológica (US-004).
	 */
	public List<Assessment> history(UUID athleteId) {
		return repository.findByAthleteIdOrderByAssessmentDateAsc(athleteId);
	}

	public Assessment findById(AssessmentId id) {
		return repository.findById(id)
				.orElseThrow(() -> new AssessmentNotFoundException(id.value()));
	}

	public record RegisterAssessmentCommand(UUID userId, UUID athleteId, LocalDate assessmentDate,
			BigDecimal weightKg, BigDecimal heightCm, BigDecimal bodyFatPercentage,
			BigDecimal muscleMassKg, BigDecimal waistCm, BigDecimal hipCm) {
	}
}
