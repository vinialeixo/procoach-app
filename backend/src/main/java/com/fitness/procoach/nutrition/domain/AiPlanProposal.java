package com.fitness.procoach.nutrition.domain;

import com.fitness.procoach.coach.domain.CoachId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Representa uma proposta de plano alimentar gerada por IA (consultiva, read-only).
 *
 * <p>Regras inegociáveis aplicadas:
 * <ul>
 *   <li>A IA é apenas consultiva — o plano nasce como rascunho (DRAFT) e exige aprovação do coach.</li>
 *   <li>Nunca aprova sozinho: {@code requiresCoachApproval} é sempre {@code true}.</li>
 *   <li>Toda persistência gera registro em {@code AuditLog} com tipo {@code AI_PLAN_PROPOSED}.</li>
 *   <li>O plano segue o contrato JSON v1 definido em {@code docs/nutricao-plano-alimentar.md §4}.</li>
 * </ul>
 *
 * <p>Framework-agnostic: nenhuma anotação Spring/JPA.
 */
public class AiPlanProposal {

    private final UUID id;
    private final CoachId coachId;
    private final AthleteId athleteId;
    private final DietObjective objective;
    private final Macronutrients targets; // macros alvo do plano (calories, protein, carbs, fat)
    private final String planMode; // MACRO_BUDGET, MEAL_PLAN, HYBRID
    private final List<AiPlanMeal> meals;
    private final List<AiPlanSupplement> supplements;
    private final List<DietWarning> warnings;
    private final AiPlanDerived derived;
    private final AiPlanConsistency consistency;
    private final Instant generatedAt;
    private final String source; // "AI_ASSISTANT"
    private final boolean requiresCoachApproval;
    private final DietId baseDietVersionId;
    private final String disclaimer;
    private final String strategyNotes;
    private final String phase;
    private final boolean trainingDay;

    private AiPlanProposal(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id");
        this.coachId = Objects.requireNonNull(builder.coachId, "coachId");
        this.athleteId = Objects.requireNonNull(builder.athleteId, "athleteId");
        this.objective = Objects.requireNonNull(builder.objective, "objective");
        this.targets = Objects.requireNonNull(builder.targets, "targets");
        this.planMode = Objects.requireNonNullElse(builder.planMode, "MEAL_PLAN");
        this.meals = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNullElse(builder.meals, List.of())));
        this.supplements = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNullElse(builder.supplements, List.of())));
        this.warnings = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNullElse(builder.warnings, List.of())));
        this.derived = Objects.requireNonNull(builder.derived, "derived");
        this.consistency = Objects.requireNonNull(builder.consistency, "consistency");
        this.generatedAt = Objects.requireNonNullElse(builder.generatedAt, Instant.now());
        this.source = Objects.requireNonNullElse(builder.source, "AI_ASSISTANT");
        this.requiresCoachApproval = true; // SEMPRE true — regra inegociável
        this.baseDietVersionId = builder.baseDietVersionId;
        this.disclaimer = Objects.requireNonNullElse(builder.disclaimer, "Plano consultivo. A decisão final e a aprovação são do coach.");
        this.strategyNotes = builder.strategyNotes;
        this.phase = builder.phase;
        this.trainingDay = builder.trainingDay;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Converte esta proposta de IA em uma nova versão de {@link Diet} em estado DRAFT (v1).
     * Usa {@link Diet#createFromAiProposal} para criar a dieta com refeições e warnings populados.
     */
    public Diet toDiet(UUID createdBy) {
        String changeReason = "Proposta gerada por IA (" + generatedAt + "): " + (strategyNotes != null ? strategyNotes : "");
        List<Meal> domainMeals = convertMeals();
        return Diet.createFromAiProposal(coachId, athleteId, objective, targets, changeReason, createdBy, domainMeals, warnings);
    }

    /**
     * Cria uma nova versão encadeada a partir de uma dieta base, usando esta proposta de IA.
     * A dieta base é congelada (readOnly=true) dentro de {@link Diet#createNewVersionFromAiProposal}.
     */
    public Diet toNewVersion(Diet baseDiet, UUID changedBy) {
        String changeReason = "Proposta gerada por IA (" + generatedAt + "): " + (strategyNotes != null ? strategyNotes : "");
        List<Meal> domainMeals = convertMeals();
        return baseDiet.createNewVersionFromAiProposal(objective, targets, changeReason, changedBy, domainMeals, warnings);
    }

    /**
     * Converte as refeições da proposta para objetos de domínio Meal.
     */
    private List<Meal> convertMeals() {
        List<Meal> domainMeals = new ArrayList<>();
        for (AiPlanMeal mealProposal : meals) {
            Meal meal = Meal.create(mealProposal.name(), mealProposal.order(), mealProposal.time(), mealProposal.notes(),
                    mealProposal.target() != null ? Macronutrients.of(
                            mealProposal.target().calories(),
                            mealProposal.target().protein(),
                            mealProposal.target().carbohydrates(),
                            mealProposal.target().fat()) : null);

            for (AiPlanMealItem itemProposal : mealProposal.items()) {
                MealItem item = MealItem.create(
                        itemProposal.foodId(),
                        itemProposal.name(),
                        itemProposal.source(),
                        itemProposal.kind(),
                        itemProposal.state(),
                        itemProposal.quantityG(),
                        itemProposal.householdMeasure(),
                        itemProposal.preparation(),
                        Macronutrients.of(
                                itemProposal.macros().calories(),
                                itemProposal.macros().protein(),
                                itemProposal.macros().carbohydrates(),
                                itemProposal.macros().fat()));
                meal.addItem(item);
            }
            domainMeals.add(meal);
        }
        return domainMeals;
    }

    // Getters
    public UUID id() { return id; }
    public CoachId coachId() { return coachId; }
    public AthleteId athleteId() { return athleteId; }
    public DietObjective objective() { return objective; }
    public Macronutrients targets() { return targets; }
    public String planMode() { return planMode; }
    public List<AiPlanMeal> meals() { return meals; }
    public List<AiPlanSupplement> supplements() { return supplements; }
    public List<DietWarning> warnings() { return warnings; }
    public AiPlanDerived derived() { return derived; }
    public AiPlanConsistency consistency() { return consistency; }
    public Instant generatedAt() { return generatedAt; }
    public String source() { return source; }
    public boolean requiresCoachApproval() { return requiresCoachApproval; }
    public DietId baseDietVersionId() { return baseDietVersionId; }
    public String disclaimer() { return disclaimer; }
    public String strategyNotes() { return strategyNotes; }
    public String phase() { return phase; }
    public boolean trainingDay() { return trainingDay; }

    // --- Value Objects aninhados ---

    /**
     * Refeição no plano proposto pela IA.
     */
    public record AiPlanMeal(
            String mealId,
            String name,
            int order,
            String time,
            String notes,
            MealTarget target,
            List<AiPlanMealItem> items,
            Macronutrients totals
    ) {
        public AiPlanMeal {
            items = Objects.requireNonNullElse(items, List.of());
        }
    }

    /**
     * Item de refeição no plano proposto pela IA.
     */
    public record AiPlanMealItem(
            String foodId,
            String name,
            FoodSource source,
            FoodKind kind,
            FoodState state,
            double quantityG,
            String householdMeasure,
            String preparation,
            Macronutrients macros
    ) {
    }

    /**
     * Metas de macros por refeição (request DTO).
     */
    public record MealTarget(double calories, double protein, double carbohydrates, double fat) {
    }

    /**
     * Suplemento no plano proposto pela IA.
     */
    public record AiPlanSupplement(
            String name,
            String dose,
            String timing,
            boolean countsTowardMacros
    ) {
    }

    /**
     * Macros derivados do plano (g/kg, %kcal, kcal/kg).
     */
    public record AiPlanDerived(
            double tmbKcal,
            double getKcal,
            double balanceKcal,
            double balancePercent,
            String classification,
            double proteinGPerKg,
            double carbohydratesGPerKg,
            double fatGPerKg,
            double proteinPctKcal,
            double carbohydratesPctKcal,
            double fatPctKcal
    ) {
    }

    /**
     * Consistência calórica 4/4/9.
     */
    public record AiPlanConsistency(
            double calculatedCalories,
            double declaredCalories,
            double deltaKcal,
            double deltaPercent,
            boolean isConsistent
    ) {
    }

    public static class Builder {
        private UUID id = UUID.randomUUID();
        private CoachId coachId;
        private AthleteId athleteId;
        private DietObjective objective;
        private Macronutrients targets;
        private String planMode;
        private List<AiPlanMeal> meals;
        private List<AiPlanSupplement> supplements;
        private List<DietWarning> warnings;
        private AiPlanDerived derived;
        private AiPlanConsistency consistency;
        private Instant generatedAt;
        private String source;
        private DietId baseDietVersionId;
        private String disclaimer;
        private String strategyNotes;
        private String phase;
        private boolean trainingDay;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder coachId(CoachId coachId) { this.coachId = coachId; return this; }
        public Builder athleteId(AthleteId athleteId) { this.athleteId = athleteId; return this; }
        public Builder objective(DietObjective objective) { this.objective = objective; return this; }
        public Builder targets(Macronutrients targets) { this.targets = targets; return this; }
        public Builder planMode(String planMode) { this.planMode = planMode; return this; }
        public Builder meals(List<AiPlanMeal> meals) { this.meals = meals; return this; }
        public Builder supplements(List<AiPlanSupplement> supplements) { this.supplements = supplements; return this; }
        public Builder warnings(List<DietWarning> warnings) { this.warnings = warnings; return this; }
        public Builder derived(AiPlanDerived derived) { this.derived = derived; return this; }
        public Builder consistency(AiPlanConsistency consistency) { this.consistency = consistency; return this; }
        public Builder generatedAt(Instant generatedAt) { this.generatedAt = generatedAt; return this; }
        public Builder source(String source) { this.source = source; return this; }
        public Builder baseDietVersionId(DietId baseDietVersionId) { this.baseDietVersionId = baseDietVersionId; return this; }
        public Builder disclaimer(String disclaimer) { this.disclaimer = disclaimer; return this; }
        public Builder strategyNotes(String strategyNotes) { this.strategyNotes = strategyNotes; return this; }
        public Builder phase(String phase) { this.phase = phase; return this; }
        public Builder trainingDay(boolean trainingDay) { this.trainingDay = trainingDay; return this; }

        public AiPlanProposal build() {
            return new AiPlanProposal(this);
        }
    }
}