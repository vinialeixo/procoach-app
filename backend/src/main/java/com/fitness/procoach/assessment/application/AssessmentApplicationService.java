package com.fitness.procoach.assessment.application;

import com.fitness.procoach.audit.application.AuditData;
import com.fitness.procoach.audit.application.AuditService;
import com.fitness.procoach.assessment.domain.Assessment;
import com.fitness.procoach.assessment.domain.AssessmentId;
import com.fitness.procoach.assessment.domain.AssessmentMetrics;
import com.fitness.procoach.assessment.domain.AssessmentMetricsCalculator;
import com.fitness.procoach.assessment.domain.AssessmentRepository;
import com.fitness.procoach.assessment.domain.BodyMeasurements;
import com.fitness.procoach.athlete.application.AthleteAccessService;
import com.fitness.procoach.coach.application.CoachApplicationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service do módulo assessment. Orquestra o domínio e a porta de
 * persistência. Framework-agnostic: registrado como bean pela configuração da
 * camada de infraestrutura.
 */
public class AssessmentApplicationService {

	private final AssessmentRepository repository;
	private final CoachApplicationService coachService;
	private final AthleteAccessService athleteAccessService;
	private final AuditService auditService;

	public AssessmentApplicationService(AssessmentRepository repository, CoachApplicationService coachService,
			AthleteAccessService athleteAccessService, AuditService auditService) {
		this.auditService = auditService;
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
	@Transactional
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
		Assessment saved = repository.save(assessment);
		auditService.record("Assessment", saved.id().value(), saved.athleteId(), "ASSESSMENT_REGISTERED",
				command.userId(), AuditData.of("assessmentDate", saved.assessmentDate()));
		return saved;
	}

	/**
	 * Histórico de avaliações de um atleta em ordem cronológica (US-004).
	 */
	public List<Assessment> history(UUID athleteId) {
		return repository.findByAthleteIdOrderByAssessmentDateAsc(athleteId);
	}

	/** Avaliação com as métricas derivadas (T-025). */
	public record AssessmentView(Assessment assessment, AssessmentMetrics metrics) {
	}

	/**
	 * Histórico cronológico com IMC, TMB, GET e evolução de peso (T-025/US-05). A
	 * variação de peso de cada avaliação é em relação à anterior do atleta.
	 */
	public List<AssessmentView> historyWithMetrics(UUID athleteId) {
		List<Assessment> assessments = history(athleteId);
		if (assessments.isEmpty()) {
			return List.of();
		}
		AthleteAccessService.AthleteProfile profile = athleteAccessService.profileOf(athleteId);
		List<AssessmentView> views = new ArrayList<>(assessments.size());
		BigDecimal previousWeight = null;
		for (Assessment assessment : assessments) {
			AssessmentMetrics metrics = AssessmentMetricsCalculator.calculate(assessment.measurements(),
					assessment.assessmentDate(), profile.male(), profile.birthDate(), previousWeight);
			views.add(new AssessmentView(assessment, metrics));
			previousWeight = assessment.measurements().weightKg();
		}
		return views;
	}

	/** Gasto energético do atleta na avaliação mais recente (T-030). */
	public record EnergyProfile(LocalDate assessmentDate, BigDecimal weightKg, BigDecimal tmbKcal,
			BigDecimal getKcal) {
	}

	/**
	 * TMB e GET da avaliação mais recente do atleta, ou vazio se ele ainda não tem
	 * nenhuma. A posse do atleta deve ter sido validada pelo chamador.
	 */
	public Optional<EnergyProfile> latestEnergyProfile(UUID athleteId) {
		List<AssessmentView> views = historyWithMetrics(athleteId);
		if (views.isEmpty()) {
			return Optional.empty();
		}
		AssessmentView latest = views.get(views.size() - 1);
		return Optional.of(new EnergyProfile(latest.assessment().assessmentDate(),
				latest.assessment().measurements().weightKg(), latest.metrics().tmbKcal(),
				latest.metrics().getKcal()));
	}

	/** Uma avaliação com suas métricas (a anterior vem do histórico do mesmo atleta). */
	public AssessmentView withMetrics(Assessment assessment) {
		return historyWithMetrics(assessment.athleteId()).stream()
				.filter(view -> view.assessment().id().equals(assessment.id()))
				.findFirst()
				.orElseThrow(() -> new AssessmentNotFoundException(assessment.id().value()));
	}

	public Assessment findById(AssessmentId id) {
		return repository.findById(id)
				.orElseThrow(() -> new AssessmentNotFoundException(id.value()));
	}

	/**
	 * Busca uma avaliação pelo id garantindo que pertence a um atleta da
	 * carteira do coach autenticado (T-058). Avaliação inexistente OU de
	 * atleta de outro coach resulta em 404 — não vaza a existência do recurso
	 * (anti-IDOR: o id do recurso nunca é suficiente para acessá-lo).
	 */
	public Assessment findOwnedById(AssessmentId id, UUID userId) {
		Assessment assessment = repository.findById(id)
				.orElseThrow(() -> new AssessmentNotFoundException(id.value()));
		UUID coachId = coachService.requireCoachIdByUserId(userId).value();
		athleteAccessService.requireOwnedByCoach(assessment.athleteId(), coachId);
		return assessment;
	}

	public record RegisterAssessmentCommand(UUID userId, UUID athleteId, LocalDate assessmentDate,
			BigDecimal weightKg, BigDecimal heightCm, BigDecimal bodyFatPercentage,
			BigDecimal muscleMassKg, BigDecimal waistCm, BigDecimal hipCm) {
	}
}
