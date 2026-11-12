package com.fitness.procoach.athlete.application;

import com.fitness.procoach.audit.application.AuditData;
import com.fitness.procoach.audit.application.AuditService;
import com.fitness.procoach.athlete.domain.Athlete;
import com.fitness.procoach.athlete.domain.AthleteStatus;
import com.fitness.procoach.athlete.domain.AthleteId;
import com.fitness.procoach.athlete.domain.AthleteRepository;
import com.fitness.procoach.athlete.domain.Goal;
import com.fitness.procoach.athlete.domain.Height;
import com.fitness.procoach.athlete.domain.Name;
import com.fitness.procoach.athlete.domain.Sex;
import com.fitness.procoach.athlete.domain.Weight;
import com.fitness.procoach.coach.application.CoachApplicationService;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service do módulo athlete (T-018). Orquestra o domínio e a porta
 * de persistência. Framework-agnostic: registrado como bean pela configuração
 * da camada de infraestrutura.
 */
public class AthleteApplicationService implements ApplicationContextAware {

	private final AthleteRepository repository;
	private final CoachApplicationService coachService;
	private final AuditService auditService;
	private final AthleteAccessInfoProvider accessInfo;
	private ApplicationContext applicationContext;

	public AthleteApplicationService(AthleteRepository repository, CoachApplicationService coachService,
			AuditService auditService, AthleteAccessInfoProvider accessInfo) {
		this.auditService = auditService;
		this.repository = repository;
		this.coachService = coachService;
		this.accessInfo = accessInfo;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	// Dynamic lookup helpers using reflection to avoid compile-time module dependencies (T-020/Modulith)
	private Object getTrainingQueryService() {
		return applicationContext.getBean("trainingQueryService");
	}

	private Object getDietApplicationService() {
		return applicationContext.getBean("dietApplicationService");
	}

	private Object getAssessmentApplicationService() {
		return applicationContext.getBean("assessmentApplicationService");
	}

	private static Object invokeMethod(Object target, String methodName, Object... args) {
		try {
			Method method = target.getClass().getMethod(methodName, getParameterTypes(args));
			return method.invoke(target, args);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Failed to invoke " + methodName, e);
		}
	}

	private static Class<?>[] getParameterTypes(Object... args) {
		Class<?>[] types = new Class<?>[args.length];
		for (int i = 0; i < args.length; i++) {
			types[i] = args[i] != null ? args[i].getClass() : Object.class;
		}
		return types;
	}

	private static boolean getBooleanField(Object target, String fieldName) {
		try {
			java.lang.reflect.Field field = target.getClass().getField(fieldName);
			return field.getBoolean(target);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Failed to read field " + fieldName, e);
		}
	}

	private static Object getEnumConstant(Class<?> enumClass, String constantName) {
		try {
			return Enum.valueOf((Class<Enum>) enumClass, constantName);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException("Enum constant not found: " + constantName, e);
		}
	}

	/**
	 * Cadastra um novo atleta (US-001). O {@link AthleteId} único e imutável é
	 * gerado aqui (US-002); o status inicial é {@code ACTIVE}. O coachId NUNCA
	 * vem do request (T-058): é resolvido server-side a partir do usuário
	 * autenticado (subject do JWT) via fachada do módulo coach.
	 */
	@Transactional
	public Athlete register(RegisterAthleteCommand command) {
		UUID coachId = coachService.requireCoachIdByUserId(command.userId()).value();
		// T-078: e-mail opcional; se já pertencer a um User, 409 acionável em vez
		// de deixar o conflito aparecer só na hora do convite.
		if (command.email() != null && !command.email().isBlank()
				&& accessInfo.emailBelongsToUser(command.email().trim())) {
			throw new EmailAlreadyRegisteredAsUserException(command.email().trim());
		}
		Athlete athlete = Athlete.register(
				AthleteId.generate(),
				coachId,
				new Name(command.name()),
				command.sex(),
				command.birthDate(),
				new Height(command.height()),
				new Weight(command.weight()),
				command.goal());
		Athlete saved = repository.save(athlete);
		auditService.record("Athlete", saved.id().value(), saved.id().value(), "ATHLETE_REGISTERED",
				command.userId(), AuditData.of("goal", saved.goal().name()));
		return saved;
	}

	/**
	 * Remove um atleta da carteira do coach autenticado (T-058). Exclusão é
	 * sempre soft-delete ({@link Athlete#softDelete()}) — o registro nunca é
	 * removido fisicamente. Lança {@link AthleteNotFoundException} (404) se o
	 * atleta não existe, foi removido ou pertence a outro coach.
	 */
	@Transactional
	public void remove(RemoveAthleteCommand command) {
		UUID coachId = coachService.requireCoachIdByUserId(command.userId()).value();
		Athlete athlete = repository.findById(AthleteId.of(command.athleteId()))
				.filter(a -> !a.isDeleted())
				.orElseThrow(() -> new AthleteNotFoundException(command.athleteId()));
		if (!athlete.coachId().equals(coachId)) {
			throw new AthleteNotFoundException(command.athleteId());
		}
		athlete.softDelete();
		repository.save(athlete);
		auditService.record("Athlete", athlete.id().value(), athlete.id().value(), "ATHLETE_DELETED",
				command.userId(), AuditData.of());
	}

	/** Atletas da carteira do coach autenticado, com filtro opcional por status. */
	public List<Athlete> list(UUID userId, AthleteStatus status) {
		UUID coachId = coachService.requireCoachIdByUserId(userId).value();
		return repository.findActiveByCoachId(coachId).stream()
				.filter(a -> status == null || a.status() == status)
				.toList();
	}

	/** Histórico de auditoria do atleta (mais recente primeiro); só para o coach dono. */
	@Transactional(readOnly = true)
	public List<AuditService.AuditEntry> history(UUID userId, UUID athleteId) {
		UUID coachId = coachService.requireCoachIdByUserId(userId).value();
		requireOwned(athleteId, coachId);
		return auditService.entriesByAthlete(athleteId);
	}

	/**
	 * Resumo dos protocolos do atleta (T-020/US-004): TrainingPlans, Diets e Assessments.
	 * Indica quais estão ativos (TrainingPlan: ACTIVE; Diet: APPROVED; Assessment: mais recente).
	 * Usa reflexão para evitar dependências de compilação entre módulos (Spring Modulith).
	 */
	@Transactional(readOnly = true)
	public ProtocolSummary protocolSummary(UUID userId, UUID athleteId) {
		UUID coachId = coachService.requireCoachIdByUserId(userId).value();
		requireOwned(athleteId, coachId);

		// TrainingPlans via reflection
		Object trainingQueryService = getTrainingQueryService();
		List<?> trainingPlans = (List<?>) invokeMethod(trainingQueryService, "findTrainingPlansByAthlete", athleteId);
		List<ProtocolSummary.TrainingPlanSummary> tpSummaries = new ArrayList<>();
		for (Object tp : trainingPlans) {
			boolean deleted = (Boolean) invokeMethod(tp, "isDeleted");
			if (deleted) continue;
			UUID id = (UUID) invokeMethod(tp, "getId");
			String name = (String) invokeMethod(tp, "getName");
			Object status = invokeMethod(tp, "getStatus");
			String statusName = (String) invokeMethod(status, "name");
			boolean active = status == getEnumConstant(status.getClass(), "ACTIVE");
			UUID periodizationId = (UUID) invokeMethod(tp, "getPeriodizationId");
			int version = (Integer) invokeMethod(tp, "getVersion");
			tpSummaries.add(new ProtocolSummary.TrainingPlanSummary(id, name, statusName, active, periodizationId, version));
		}

		// Diets via reflection
		Object dietApplicationService = getDietApplicationService();
		List<?> diets = (List<?>) invokeMethod(dietApplicationService, "historyByAthlete", athleteId);
		List<ProtocolSummary.DietSummary> dietSummaries = new ArrayList<>();
		for (Object d : diets) {
			boolean deleted = (Boolean) invokeMethod(d, "isDeleted");
			if (deleted) continue;
			Object idObj = invokeMethod(d, "id");
			UUID id = (UUID) invokeMethod(idObj, "value");
			Object objective = invokeMethod(d, "objective");
			String objectiveName = (String) invokeMethod(objective, "name");
			Object status = invokeMethod(d, "status");
			String statusName = (String) invokeMethod(status, "name");
			boolean active = status == getEnumConstant(status.getClass(), "APPROVED");
			int version = (Integer) invokeMethod(d, "version");
			dietSummaries.add(new ProtocolSummary.DietSummary(id, objectiveName, statusName, active, version));
		}

		// Assessments via reflection
		Object assessmentApplicationService = getAssessmentApplicationService();
		List<?> assessments = (List<?>) invokeMethod(assessmentApplicationService, "history", athleteId);
		List<ProtocolSummary.AssessmentSummary> assessmentSummaries = new ArrayList<>();
		for (Object a : assessments) {
			Object idObj = invokeMethod(a, "id");
			UUID id = (UUID) invokeMethod(idObj, "value");
			Object dateObj = invokeMethod(a, "assessmentDate");
			String dateStr = dateObj.toString();
			assessmentSummaries.add(new ProtocolSummary.AssessmentSummary(id, dateStr, false));
		}
		// Marcar a mais recente
		if (!assessmentSummaries.isEmpty()) {
			int lastIdx = assessmentSummaries.size() - 1;
			ProtocolSummary.AssessmentSummary last = assessmentSummaries.get(lastIdx);
			assessmentSummaries.set(lastIdx, new ProtocolSummary.AssessmentSummary(last.id(), last.assessmentDate(), true));
		}

		return new ProtocolSummary(athleteId, tpSummaries, dietSummaries, assessmentSummaries);
	}

	/** Ficha de um atleta da carteira; 404 se inexistente, removido ou de outro coach. */
	public Athlete get(UUID userId, UUID athleteId) {
		UUID coachId = coachService.requireCoachIdByUserId(userId).value();
		return requireOwned(athleteId, coachId);
	}

	/** Troca o status do atleta (US-005); mesma regra de posse do {@link #remove}. */
	@Transactional
	public Athlete changeStatus(ChangeStatusCommand command) {
		UUID coachId = coachService.requireCoachIdByUserId(command.userId()).value();
		Athlete athlete = requireOwned(command.athleteId(), coachId);
		AthleteStatus previous = athlete.status();
		athlete.changeStatus(command.status());
		Athlete saved = repository.save(athlete);
		auditService.record("Athlete", saved.id().value(), saved.id().value(), "ATHLETE_STATUS_CHANGED",
				command.userId(), AuditData.of("from", previous.name(), "to", saved.status().name(), "reason",
						command.reason()));
		return saved;
	}

	private Athlete requireOwned(UUID athleteId, UUID coachId) {
		Athlete athlete = repository.findById(AthleteId.of(athleteId))
				.filter(a -> !a.isDeleted())
				.orElseThrow(() -> new AthleteNotFoundException(athleteId));
		if (!athlete.coachId().equals(coachId)) {
			throw new AthleteNotFoundException(athleteId);
		}
		return athlete;
	}

	public record ChangeStatusCommand(UUID userId, UUID athleteId, AthleteStatus status, String reason) {
	}

	public record RegisterAthleteCommand(UUID userId, String name, Sex sex, LocalDate birthDate,
			BigDecimal height, BigDecimal weight, Goal goal, String email) {
	}

	public record RemoveAthleteCommand(UUID userId, UUID athleteId) {
	}

	/**
	 * Resumo dos protocolos de um atleta (T-020/US-004).
	 * Agrega TrainingPlans, Diets e Assessments com indicação de status ativo.
	 */
	public record ProtocolSummary(
			UUID athleteId,
			List<TrainingPlanSummary> trainingPlans,
			List<DietSummary> diets,
			List<AssessmentSummary> assessments
	) {
		public record TrainingPlanSummary(UUID id, String name, String status, boolean active,
				UUID periodizationId, int version) {
		}

		public record DietSummary(UUID id, String objective, String status, boolean active, int version) {
		}

		public record AssessmentSummary(UUID id, String assessmentDate, boolean latest) {
		}
	}
}
