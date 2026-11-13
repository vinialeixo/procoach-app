package com.fitness.procoach.nutrition.application;

import com.fitness.procoach.athlete.application.AthleteAccessService;
import com.fitness.procoach.coach.application.CoachApplicationService;
import com.fitness.procoach.coach.domain.CoachId;
import com.fitness.procoach.assessment.application.AssessmentApplicationService;
import com.fitness.procoach.audit.application.AuditData;
import com.fitness.procoach.notification.application.NotificationService;
import com.fitness.procoach.audit.application.AuditService;
import com.fitness.procoach.nutrition.domain.AiPlanProposal;
import com.fitness.procoach.nutrition.domain.AthleteId;
import com.fitness.procoach.nutrition.domain.Diet;
import com.fitness.procoach.nutrition.domain.DietId;
import com.fitness.procoach.nutrition.domain.DietWarning;
import com.fitness.procoach.nutrition.domain.EnergyBalance;
import com.fitness.procoach.nutrition.domain.EnergyBalanceCalculator;
import com.fitness.procoach.nutrition.domain.DietObjective;
import com.fitness.procoach.nutrition.domain.DietRepository;
import com.fitness.procoach.nutrition.domain.DietStatus;
import com.fitness.procoach.nutrition.domain.FoodKind;
import com.fitness.procoach.nutrition.domain.FoodSource;
import com.fitness.procoach.nutrition.domain.FoodState;
import com.fitness.procoach.nutrition.domain.Macronutrients;
import com.fitness.procoach.nutrition.domain.Meal;
import com.fitness.procoach.nutrition.domain.MealId;
import com.fitness.procoach.nutrition.domain.MealItem;
import com.fitness.procoach.nutrition.domain.MealItemId;
import com.fitness.procoach.nutrition.domain.Supplement;
import java.time.Instant;
import java.util.List;
import java.util.Comparator;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service do módulo nutrition. Orquestra o domínio e a porta de
 * persistência. Framework-agnostic: registrado como bean pela configuração da
 * camada de infraestrutura.
 */
public class DietApplicationService {

	private final DietRepository repository;
	private final CoachApplicationService coachService;
	private final AthleteAccessService athleteAccessService;
	private final AssessmentApplicationService assessmentService;
	private final AuditService auditService;
	private final NotificationService notificationService;

	public DietApplicationService(DietRepository repository, CoachApplicationService coachService,
			AthleteAccessService athleteAccessService, AssessmentApplicationService assessmentService,
			AuditService auditService, NotificationService notificationService) {
		this.notificationService = notificationService;
		this.auditService = auditService;
		this.assessmentService = assessmentService;
		this.repository = repository;
		this.coachService = coachService;
		this.athleteAccessService = athleteAccessService;
	}

	/**
	 * Cria a primeira versão (v1) de uma dieta, em estado Rascunho (US-007).
	 * Objetivo e macronutrientes são obrigatórios. O coachId NUNCA vem do
	 * request (T-058): é resolvido server-side a partir do usuário autenticado,
	 * e o atleta-alvo precisa pertencer a esse coach (senão 404). O createdBy
	 * (auditoria) também nunca vem do request: é o próprio subject do JWT.
	 */
	@Transactional
	public Diet createDiet(CreateDietCommand command) {
		CoachId coachId = coachService.requireCoachIdByUserId(command.userId());
		athleteAccessService.requireOwnedByCoach(command.athleteId(), coachId.value());
		// Uma cadeia de versões por atleta: a primeira dieta é v1; depois só nova versão.
		if (repository.findLatestByAthleteId(AthleteId.of(command.athleteId())).isPresent()) {
			throw new IllegalStateException(
					"Athlete already has a diet; create a new version of the existing one");
		}
		Diet diet = Diet.create(
				coachId,
				AthleteId.of(command.athleteId()),
				command.objective(),
				Macronutrients.of(command.calories(), command.protein(),
						command.carbohydrates(), command.fat()),
				command.userId());
		Diet saved = repository.save(diet);
		auditService.record("Diet", saved.id().value(), saved.athleteId().value(), "DIET_CREATED", command.userId(),
				AuditData.of("objective", saved.objective().name(), "version", saved.version()));
		return saved;
	}

	/**
	 * Retorna a versão mais recente da dieta de um atleta (US-007).
	 */
	public Diet latestByAthlete(UUID athleteId) {
		return repository.findLatestByAthleteId(AthleteId.of(athleteId))
				.orElseThrow(() -> new DietNotFoundException(athleteId));
	}

	/**
	 * Histórico de versões da dieta de um atleta.
	 */
	public List<Diet> historyByAthlete(UUID athleteId) {
		return repository.findAllVersionsByAthleteId(AthleteId.of(athleteId));
	}

	/**
	 * Versão corrente da dieta. Coach: a mais recente (rascunho, revisão ou
	 * aprovada). Atleta: a versão APROVADA mais recente — o que o coach ainda está
	 * montando ou revisando não aparece (US-038).
	 * <p>
	 * Regra T-089: se a versão mais alta da cadeia estiver ARCHIVED, não cai para
	 * uma anterior — responde 404 (sem plano ativo). Isso corrige a regressão em
	 * que o atleta voltava a ver a v1 depois de a v2 ser arquivada.
	 */
	public Diet latestByAthlete(UUID athleteId, boolean athleteView) {
		if (!athleteView) {
			return latestByAthlete(athleteId);
		}
		List<Diet> athleteHistory = historyByAthlete(athleteId, true);
		if (athleteHistory.isEmpty()) {
			throw new DietNotFoundException(athleteId);
		}
		// A versão mais alta da cadeia (maior version) define a regra
		Diet highest = athleteHistory.stream()
				.max(Comparator.comparingInt(Diet::version))
				.orElseThrow(() -> new DietNotFoundException(athleteId));

		if (highest.status() == DietStatus.ARCHIVED) {
			// Regra T-089: versão mais alta arquivada = 404, não cai para anterior
			throw new DietNotFoundException(athleteId);
		}

		// Caso contrário, retorna a maior versão APPROVED (como antes)
		return athleteHistory.stream()
				.filter(d -> d.status() == DietStatus.APPROVED)
				.max(Comparator.comparingInt(Diet::version))
				.orElseThrow(() -> new DietNotFoundException(athleteId));
	}

	/**
	 * Histórico de versões. Atleta: só versões que já foram aprovadas (APPROVED ou
	 * ARCHIVED, que só se alcança a partir de APPROVED) e não removidas.
	 */
	public List<Diet> historyByAthlete(UUID athleteId, boolean athleteView) {
		List<Diet> all = historyByAthlete(athleteId);
		if (!athleteView) {
			return all;
		}
		return all.stream()
				.filter(d -> !d.isDeleted())
				.filter(d -> d.status() == DietStatus.APPROVED || d.status() == DietStatus.ARCHIVED)
				.toList();
	}

	public Diet findById(DietId id) {
		return repository.findById(id)
				.orElseThrow(() -> new DietNotFoundException(id.value()));
	}

	/**
	 * Busca uma dieta pelo id garantindo que pertence a um atleta da carteira
	 * do coach autenticado (T-058). Dieta inexistente OU de atleta de outro
	 * coach resulta em 404 — não vaza a existência do recurso (anti-IDOR: o id
	 * do recurso nunca é suficiente para acessá-lo).
	 */
	public Diet findOwnedById(DietId id, UUID userId) {
		Diet diet = repository.findById(id)
				.orElseThrow(() -> new DietNotFoundException(id.value()));
		CoachId coachId = coachService.requireCoachIdByUserId(userId);
		athleteAccessService.requireOwnedByCoach(diet.athleteId().value(), coachId.value());
		return diet;
	}

	/**
	 * Cria a próxima versão a partir de {@code dietId} (US-015/US-016): a versão
	 * de origem é congelada (readOnly) e o motivo da alteração é obrigatório.
	 * Mesma regra de posse do {@link #findOwnedById}.
	 */
	@Transactional
	public Diet createNewVersion(NewVersionCommand command) {
		Diet current = findOwnedById(new DietId(command.dietId()), command.userId());
		Diet next = current.createNewVersion(
				command.objective(),
				Macronutrients.of(command.calories(), command.protein(), command.carbohydrates(), command.fat()),
				command.changeReason(),
				command.userId());
		repository.save(current);
		Diet saved = repository.save(next);
		auditService.record("Diet", saved.id().value(), saved.athleteId().value(), "DIET_VERSIONED", command.userId(),
				AuditData.of("version", saved.version(), "previousVersionId", current.id().value(), "reason",
						command.changeReason()));
		return saved;
	}

	/** Transiciona o status da dieta (US-017); transição inválida lança IllegalStateException. */
	@Transactional
	public Diet changeStatus(UUID userId, UUID dietId, DietStatus status) {
		Diet diet = findOwnedById(new DietId(dietId), userId);
		DietStatus previous = diet.status();
		diet.changeStatus(status);
		Diet saved = repository.save(diet);
		auditService.record("Diet", saved.id().value(), saved.athleteId().value(), "DIET_STATUS_CHANGED", userId,
				AuditData.of("from", previous.name(), "to", saved.status().name(), "version", saved.version()));
		if (saved.status() == DietStatus.APPROVED) {
			// Aprovada: é o que o atleta passa a enxergar. Avisa na mesma transação.
			notificationService.notifyAthlete(saved.athleteId().value(), "DIET_APPROVED",
					saved.version() > 1
							? "Seu coach atualizou sua dieta (versão " + saved.version() + "). Confira as novas metas."
							: "Sua dieta está disponível.",
					"Diet", saved.id().value());
		}
		return saved;
	}

	/** Dieta com o balanço energético calculado e a avaliação que o embasou. */
	public record DietEnergyBalance(Diet diet, AssessmentApplicationService.EnergyProfile profile,
			EnergyBalance balance) {
	}

	/**
	 * Déficit/superávit da dieta em relação ao GET da avaliação mais recente do
	 * atleta (T-030/US-07). Mesma regra de posse do {@link #findOwnedById}; se o
	 * atleta não tem avaliação, não há GET e a resposta é 409.
	 */
	public DietEnergyBalance energyBalance(UUID dietId, UUID userId) {
		return balanceOf(findOwnedById(new DietId(dietId), userId));
	}

	/**
	 * Balanço da versão mais recente da dieta do atleta (visão do atleta e do
	 * coach). A posse/autorização do atleta deve ter sido validada pelo chamador
	 * (guard do JWT e, para coach, a carteira). Atleta sem dieta resulta em 404.
	 * Atleta sem avaliação resulta em 409 acionável (T-089).
	 */
	public DietEnergyBalance latestEnergyBalance(UUID athleteId, boolean athleteView) {
		Diet diet = latestByAthlete(athleteId, athleteView);
		return balanceOf(diet);
	}

	/**
	 * Versão corrente da dieta com balanço energético calculado (T-089).
	 * Retorna a dieta + balanço, ou lança 404 se não há dieta ativa, 409 se não há avaliação.
	 */
	public DietWithEnergyBalance latestWithEnergyBalance(UUID athleteId, boolean athleteView) {
		Diet diet = latestByAthlete(athleteId, athleteView);
		AssessmentApplicationService.EnergyProfile profile = assessmentService
				.latestEnergyProfile(diet.athleteId().value())
				.orElseThrow(() -> new NoAssessmentException(diet.athleteId().value()));
		EnergyBalance balance = EnergyBalanceCalculator.calculate(diet.macros().calories(), profile.getKcal());
		return new DietWithEnergyBalance(diet, profile, balance);
	}

	/**
	 * Versão específica da dieta com balanço energético (coach).
	 */
	public DietWithEnergyBalance energyBalanceWithDiet(UUID dietId, UUID userId) {
		Diet diet = findOwnedById(new DietId(dietId), userId);
		AssessmentApplicationService.EnergyProfile profile = assessmentService
				.latestEnergyProfile(diet.athleteId().value())
				.orElseThrow(() -> new NoAssessmentException(diet.athleteId().value()));
		EnergyBalance balance = EnergyBalanceCalculator.calculate(diet.macros().calories(), profile.getKcal());
		return new DietWithEnergyBalance(diet, profile, balance);
	}

	/** Exceção lançada quando não há avaliação para calcular o balanço (409 acionável). */
	public static class NoAssessmentException extends IllegalStateException {
		public NoAssessmentException(UUID athleteId) {
			super("Athlete " + athleteId + " has no assessment; register one to calculate the energy balance");
		}
	}

	private DietEnergyBalance balanceOf(Diet diet) {
		AssessmentApplicationService.EnergyProfile profile = assessmentService
				.latestEnergyProfile(diet.athleteId().value())
				.orElseThrow(() -> new IllegalStateException(
						"Athlete has no assessment; register one to calculate the energy balance"));
		return new DietEnergyBalance(diet, profile,
				EnergyBalanceCalculator.calculate(diet.macros().calories(), profile.getKcal()));
	}

	/**
	 * Dieta com balanço energético para resposta da API (T-089).
	 */
	public record DietWithEnergyBalance(Diet diet, AssessmentApplicationService.EnergyProfile profile,
			EnergyBalance balance) {
	}

	/** Adiciona uma refeição à dieta (T-085). */
	@Transactional
	public Diet addMeal(UUID userId, UUID dietId, AddMealCommand command) {
		Diet diet = findOwnedById(new DietId(dietId), userId);
		Meal meal = Meal.create(command.name(), command.order(), command.time(), command.notes(),
				command.target() != null ? Macronutrients.of(command.target().calories(), command.target().protein(),
						command.target().carbohydrates(), command.target().fat()) : null);
		diet.addMeal(meal);
		Diet saved = repository.save(diet);
		auditService.record("Diet", saved.id().value(), saved.athleteId().value(), "MEAL_ADDED", userId,
				AuditData.of("mealId", meal.id().value(), "mealName", meal.name(), "version", saved.version()));
		return saved;
	}

	/** Adiciona um item a uma refeição da dieta (T-085). */
	@Transactional
	public Diet addMealItem(UUID userId, UUID dietId, UUID mealId, AddMealItemCommand command) {
		Diet diet = findOwnedById(new DietId(dietId), userId);
		Meal meal = diet.meals().stream()
				.filter(m -> m.id().value().equals(mealId))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Meal not found: " + mealId));

		MealItem item = MealItem.create(
				command.foodId(),
				command.name(),
				command.source(),
				command.kind(),
				command.state(),
				command.quantityG(),
				command.householdMeasure(),
				command.preparation(),
				Macronutrients.of(command.calories(), command.protein(), command.carbohydrates(), command.fat()));
		meal.addItem(item);
		diet.recalculateMacrosFromMeals();
		Diet saved = repository.save(diet);
		auditService.record("Diet", saved.id().value(), saved.athleteId().value(), "MEAL_ITEM_ADDED", userId,
				AuditData.of("mealId", mealId, "itemId", item.id().value(), "itemName", item.name(), "version", saved.version()));
		return saved;
	}

	/** Remove um item de uma refeição da dieta (T-085). */
	@Transactional
	public Diet removeMealItem(UUID userId, UUID dietId, UUID mealId, UUID itemId) {
		Diet diet = findOwnedById(new DietId(dietId), userId);
		Meal meal = diet.meals().stream()
				.filter(m -> m.id().value().equals(mealId))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Meal not found: " + mealId));

		boolean removed = meal.removeItem(new MealItemId(itemId));
		if (!removed) {
			throw new IllegalArgumentException("MealItem not found: " + itemId);
		}
		diet.recalculateMacrosFromMeals();
		Diet saved = repository.save(diet);
		auditService.record("Diet", saved.id().value(), saved.athleteId().value(), "MEAL_ITEM_REMOVED", userId,
				AuditData.of("mealId", mealId, "itemId", itemId, "version", saved.version()));
		return saved;
	}

	/** Remove uma refeição da dieta (T-085). */
	@Transactional
	public Diet removeMeal(UUID userId, UUID dietId, UUID mealId) {
		Diet diet = findOwnedById(new DietId(dietId), userId);
		String mealName = diet.meals().stream()
				.filter(m -> m.id().value().equals(mealId))
				.findFirst()
				.map(Meal::name)
				.orElse("unknown");

		boolean removed = diet.removeMeal(new MealId(mealId));
		if (!removed) {
			throw new IllegalArgumentException("Meal not found: " + mealId);
		}
		Diet saved = repository.save(diet);
		auditService.record("Diet", saved.id().value(), saved.athleteId().value(), "MEAL_REMOVED", userId,
				AuditData.of("mealId", mealId, "mealName", mealName, "version", saved.version()));
		return saved;
	}

	/** Adiciona um suplemento à dieta (T-090). */
	@Transactional
	public Diet addSupplement(UUID userId, UUID dietId, AddSupplementCommand command) {
		Diet diet = findOwnedById(new DietId(dietId), userId);
		Supplement supplement = Supplement.create(command.name(), command.dose(), command.timing(), command.countsTowardMacros());
		diet.addSupplement(supplement);
		Diet saved = repository.save(diet);
		auditService.record("Diet", saved.id().value(), saved.athleteId().value(), "SUPPLEMENT_ADDED", userId,
				AuditData.of("supplementName", supplement.name(), "countsTowardMacros", supplement.countsTowardMacros(), "version", saved.version()));
		return saved;
	}

	/** Remove um suplemento da dieta pelo nome (T-090). */
	@Transactional
	public Diet removeSupplement(UUID userId, UUID dietId, String supplementName) {
		Diet diet = findOwnedById(new DietId(dietId), userId);
		boolean removed = diet.removeSupplement(supplementName);
		if (!removed) {
			throw new IllegalArgumentException("Supplement not found: " + supplementName);
		}
		Diet saved = repository.save(diet);
		auditService.record("Diet", saved.id().value(), saved.athleteId().value(), "SUPPLEMENT_REMOVED", userId,
				AuditData.of("supplementName", supplementName, "version", saved.version()));
		return saved;
	}

	public record AddMealCommand(UUID userId, UUID dietId, String name, int order, String time, String notes,
			MealTarget target) {
	}

	public record MealTarget(double calories, double protein, double carbohydrates, double fat) {
	}

	public record AddMealItemCommand(UUID userId, UUID dietId, UUID mealId, String foodId, String name,
			FoodSource source, FoodKind kind, FoodState state, double quantityG, String householdMeasure,
			String preparation, double calories, double protein, double carbohydrates, double fat) {
	}

	public record NewVersionCommand(UUID userId, UUID dietId, DietObjective objective,
			double calories, double protein, double carbohydrates, double fat, String changeReason) {
	}

	public record CreateDietCommand(UUID userId, UUID athleteId, DietObjective objective,
			double calories, double protein, double carbohydrates, double fat) {
	}

	/**
	 * Comando para propor um plano alimentar gerado por IA (T-088).
	 * A proposta chega como JSON válido (contrato v1) e é convertida em uma dieta
	 * em estado DRAFT com {@code requiresCoachApproval = true}.
	 * Auditoria: evento {@code AI_PLAN_PROPOSED}.
	 */
	public record ProposeAiPlanCommand(UUID userId, UUID athleteId, AiPlanProposal proposal) {
	}

	/**
	 * Cria uma dieta em DRAFT a partir de uma proposta de IA (T-088).
	 * O plano nunca é aprovado automaticamente — fica em DRAFT aguardando ação do coach.
	 * Registra auditoria {@code AI_PLAN_PROPOSED}.
	 */
	@Transactional
	public Diet proposeAiPlan(ProposeAiPlanCommand command) {
		CoachId coachId = coachService.requireCoachIdByUserId(command.userId());
		athleteAccessService.requireOwnedByCoach(command.athleteId(), coachId.value());

		Diet diet;
		DietId previousVersionId = null;

		if (command.proposal().baseDietVersionId() == null) {
			// Primeira dieta do atleta
			if (repository.findLatestByAthleteId(AthleteId.of(command.athleteId())).isPresent()) {
				throw new IllegalStateException(
						"Athlete already has a diet; use baseDietVersionId to create a new version from AI proposal");
			}
			diet = command.proposal().toDiet(command.userId());
		} else {
			// Nova versão baseada em dieta existente
			Diet base = repository.findById(command.proposal().baseDietVersionId())
					.orElseThrow(() -> new DietNotFoundException(command.proposal().baseDietVersionId().value()));
			if (!base.athleteId().value().equals(command.athleteId())) {
				throw new IllegalArgumentException("baseDietVersionId does not belong to the specified athlete");
			}
			athleteAccessService.requireOwnedByCoach(base.athleteId().value(), coachId.value());

			diet = command.proposal().toNewVersion(base, command.userId());
			previousVersionId = base.id();
			repository.save(base); // salva base congelada
		}

		Diet saved = repository.save(diet);

		// Auditoria: AI_PLAN_PROPOSED
		auditService.record("Diet", saved.id().value(), saved.athleteId().value(), "AI_PLAN_PROPOSED", command.userId(),
				AuditData.of("version", saved.version(), "objective", saved.objective().name(),
						"source", "AI_ASSISTANT", "baseDietVersionId",
						previousVersionId != null ? previousVersionId.value().toString() : "none"));

		return saved;
	}

	/**
	 * Valida uma proposta de IA sem persistir (dry-run).
	 * Retorna a proposta enriquecida com warnings calculados e consistência verificada.
	 */
	public AiPlanProposal validateAiPlan(AiPlanProposal proposal, double athleteWeightKg, double tmbKcal, double getKcal) {
		// Converte para Diet temporária para reutilizar lógica de warnings/consistência
		Diet tempDiet = proposal.toDiet(UUID.randomUUID());
		Macronutrients totals = tempDiet.calculateTotalsFromMeals();

		// Calcula balanço e classificação
		double balanceKcal = totals.calories() - getKcal;
		double balancePercent = getKcal > 0 ? (balanceKcal / getKcal) * 100 : 0;
		EnergyBalance.Classification classification;
		if (balancePercent <= -5) {
			classification = EnergyBalance.Classification.DEFICIT;
		} else if (balancePercent >= 5) {
			classification = EnergyBalance.Classification.SURPLUS;
		} else {
			classification = EnergyBalance.Classification.MAINTENANCE;
		}

		// Gera warnings
		List<DietWarning> warnings = tempDiet.generateWarnings(athleteWeightKg, tmbKcal, getKcal, balancePercent, classification);

		// Derivados
		Diet.DerivedMacros derived = tempDiet.derivedMacros(athleteWeightKg);

		// Consistência
		AiPlanProposal.AiPlanConsistency consistency = new AiPlanProposal.AiPlanConsistency(
				totals.calculatedCalories(), totals.calories(),
				totals.consistencyDeltaKcal(), totals.consistencyDeltaPercent(),
				totals.isConsistent());

		// Retorna proposta enriquecida
		return AiPlanProposal.builder()
				.id(proposal.id())
				.coachId(proposal.coachId())
				.athleteId(proposal.athleteId())
				.objective(proposal.objective())
				.targets(proposal.targets())
				.planMode(proposal.planMode())
				.meals(proposal.meals())
				.supplements(proposal.supplements())
				.warnings(warnings)
				.derived(new AiPlanProposal.AiPlanDerived(
						proposal.derived().tmbKcal(),
						proposal.derived().getKcal(),
						balanceKcal,
						balancePercent,
						classification.name(),
						derived.proteinGPerKg(),
						derived.carbohydratesGPerKg(),
						derived.fatGPerKg(),
						derived.proteinPctKcal(),
						derived.carbohydratesPctKcal(),
						derived.fatPctKcal()))
				.consistency(consistency)
				.generatedAt(proposal.generatedAt())
				.source(proposal.source())
				.baseDietVersionId(proposal.baseDietVersionId())
				.disclaimer(proposal.disclaimer())
				.strategyNotes(proposal.strategyNotes())
				.phase(proposal.phase())
				.trainingDay(proposal.trainingDay())
				.build();
	}

	/** Comando para adicionar um suplemento à dieta (T-090). */
	public record AddSupplementCommand(UUID userId, UUID dietId, String name, String dose,
			String timing, boolean countsTowardMacros) {
	}
}
