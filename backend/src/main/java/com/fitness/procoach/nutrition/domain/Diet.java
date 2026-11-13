package com.fitness.procoach.nutrition.domain;

import com.fitness.procoach.coach.domain.CoachId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Agregado raiz do bounded context Nutrition. Representa uma única versão de uma dieta.
 *
 * <p>Regras inegociáveis aplicadas aqui:
 * <ul>
 *   <li>Nunca sobrescrever: cada alteração gera uma nova versão (v1 -> v2 -> v3) com
 *       histórico encadeado via {@code previousVersionId} (US-015).</li>
 *   <li>Versões anteriores tornam-se somente leitura (US-015).</li>
 *   <li>Macronutrientes obrigatórios; carboidratos e gorduras aceitam zero (T-087/US-014).</li>
 *   <li>Objetivo definido na criação (US-011).</li>
 *   <li>Exclusão lógica (soft-delete), nunca DELETE físico.</li>
 *   <li>Referências a outros agregados por UUID (coachId, athleteId).</li>
 *   <li>Totais de macros são <b>derivados dos itens das refeições</b> quando há refeições
 *       (T-085 / docs/nutricao-plano-alimentar.md §3.3).</li>
 *   <li>Warnings estruturados informativos — nunca bloqueiam (T-087).</li>
 * </ul>
 *
 * <p>Framework-agnostic: nenhuma anotação Spring/JPA neste pacote.
 */
public class Diet {

    private final DietId id;
    private final CoachId coachId;
    private final AthleteId athleteId;
    private DietObjective objective;
    private Macronutrients macros; // snapshot dos totais (metas ou derivados dos itens)
    private DietStatus status;
    private final int version;
    private final DietId previousVersionId;
    private final String changeReason;
    private final UUID createdBy;
    private UUID updatedBy;
    private final Instant createdAt;
    private Instant updatedAt;
    private boolean readOnly;
    private boolean deleted;
    private final List<Meal> meals = new ArrayList<>();
    private final List<DietWarning> warnings = new ArrayList<>();
    private final List<DietEvent> domainEvents = new ArrayList<>();

    private Diet(DietId id, CoachId coachId, AthleteId athleteId, DietObjective objective,
                 Macronutrients macros, DietStatus status, int version, DietId previousVersionId,
                 String changeReason, UUID createdBy, UUID updatedBy, Instant createdAt,
                 Instant updatedAt, boolean readOnly, boolean deleted) {
        this.id = Objects.requireNonNull(id, "id");
        this.coachId = Objects.requireNonNull(coachId, "coachId");
        this.athleteId = Objects.requireNonNull(athleteId, "athleteId");
        this.objective = Objects.requireNonNull(objective, "objective");
        this.macros = Objects.requireNonNull(macros, "macros");
        this.status = Objects.requireNonNull(status, "status");
        this.version = version;
        this.previousVersionId = previousVersionId;
        this.changeReason = changeReason;
        this.createdBy = Objects.requireNonNull(createdBy, "createdBy");
        this.updatedBy = updatedBy;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = updatedAt;
        this.readOnly = readOnly;
        this.deleted = deleted;
    }

    /**
     * Reconstitui o agregado a partir do estado persistido (uso exclusivo da
     * camada de infraestrutura).
     */
    public static Diet reconstitute(DietId id, CoachId coachId, AthleteId athleteId,
                                    DietObjective objective, Macronutrients macros,
                                    DietStatus status, int version, DietId previousVersionId,
                                    String changeReason, UUID createdBy, UUID updatedBy,
                                    Instant createdAt, Instant updatedAt, boolean readOnly,
                                    boolean deleted, List<Meal> meals, List<DietWarning> warnings) {
        Diet diet = new Diet(id, coachId, athleteId, objective, macros, status, version,
                previousVersionId, changeReason, createdBy, updatedBy, createdAt, updatedAt,
                readOnly, deleted);
        if (meals != null) {
            diet.meals.addAll(meals);
        }
        if (warnings != null) {
            diet.warnings.addAll(warnings);
        }
        return diet;
    }

    /**
     * Cria a primeira versão (v1) de uma dieta, em estado Rascunho.
     */
    public static Diet create(CoachId coachId, AthleteId athleteId, DietObjective objective,
                              Macronutrients macros, UUID createdBy) {
        Instant now = Instant.now();
        Diet diet = new Diet(DietId.random(), coachId, athleteId, objective, macros,
                DietStatus.DRAFT, 1, null, null, createdBy, createdBy, now, now, false, false);
        diet.registerEvent(new DietCreated(diet.id, coachId, athleteId, objective, macros, createdBy, now));
        return diet;
    }

    /**
     * Cria uma nova versão a partir desta. A versão atual torna-se somente leitura.
     * O motivo da alteração é obrigatório (US-016).
     * As refeições, itens e warnings são copiados (snapshot) para a nova versão.
     */
    public Diet createNewVersion(DietObjective newObjective, Macronutrients newMacros,
                                 String changeReason, UUID changedBy) {
        if (readOnly) {
            throw new IllegalStateException("Cannot create a new version from a read-only version");
        }
        if (deleted) {
            throw new IllegalStateException("Cannot create a new version from a deleted diet");
        }
        if (changeReason == null || changeReason.isBlank()) {
            throw new IllegalArgumentException("changeReason is required to create a new version");
        }
        Instant now = Instant.now();
        this.readOnly = true;
        this.updatedBy = changedBy;
        this.updatedAt = now;

        // Copia refeições e itens (deep copy para nova versão)
        List<Meal> copiedMeals = new ArrayList<>();
        for (Meal meal : this.meals) {
            List<MealItem> copiedItems = new ArrayList<>();
            for (MealItem item : meal.items()) {
                copiedItems.add(MealItem.reconstitute(
                        MealItemId.random(), item.foodId(), item.name(), item.source(),
                        item.kind(), item.state(), item.quantityG(),
                        item.householdMeasure(), item.preparation(), item.macrosSnapshot()));
            }
            Meal copiedMeal = Meal.reconstitute(
                    MealId.random(), meal.name(), meal.order(), meal.time(),
                    meal.notes(), meal.target(), copiedItems);
            copiedMeals.add(copiedMeal);
        }

        // Copia warnings (snapshot)
        List<DietWarning> copiedWarnings = new ArrayList<>(this.warnings);

        Diet next = new Diet(DietId.random(), coachId, athleteId, newObjective, newMacros,
                DietStatus.DRAFT, version + 1, id, changeReason, changedBy, changedBy, now, now, false, false);
        next.meals.addAll(copiedMeals);
        next.warnings.addAll(copiedWarnings);

        // Recalcula macros da nova versão a partir dos itens copiados
        next.recalculateMacrosFromMeals();

        next.registerEvent(new DietVersioned(next.id, id, version + 1, newObjective, newMacros,
                changeReason, changedBy, now));
        return next;
    }

    /**
     * Adiciona uma refeição à dieta.
     */
    public void addMeal(Meal meal) {
        Objects.requireNonNull(meal, "meal");
        if (readOnly) {
            throw new IllegalStateException("Cannot add meal to a read-only version");
        }
        if (deleted) {
            throw new IllegalStateException("Cannot add meal to a deleted diet");
        }
        meals.add(meal);
        recalculateMacrosFromMeals();
        this.updatedAt = Instant.now();
    }

    /**
     * Remove uma refeição da dieta pelo id.
     */
    public boolean removeMeal(MealId mealId) {
        if (readOnly) {
            throw new IllegalStateException("Cannot remove meal from a read-only version");
        }
        if (deleted) {
            throw new IllegalStateException("Cannot remove meal from a deleted diet");
        }
        boolean removed = meals.removeIf(m -> m.id().equals(mealId));
        if (removed) {
            recalculateMacrosFromMeals();
            this.updatedAt = Instant.now();
        }
        return removed;
    }

    /**
     * Recalcula os macros da dieta a partir dos itens das refeições.
     * Chamado automaticamente ao adicionar/remover refeições ou itens.
     */
    public void recalculateMacrosFromMeals() {
        if (meals.isEmpty()) {
            return; // mantém macros informados manualmente (modo MACRO_BUDGET)
        }
        double calories = 0;
        double protein = 0;
        double carbohydrates = 0;
        double fat = 0;
        boolean hasAnyItems = false;
        for (Meal meal : meals) {
            Macronutrients totals = meal.calculateTotals();
            if (totals != null) {
                calories += totals.calories();
                protein += totals.protein();
                carbohydrates += totals.carbohydrates();
                fat += totals.fat();
                hasAnyItems = true;
            }
        }
        if (hasAnyItems) {
            this.macros = Macronutrients.of(calories, protein, carbohydrates, fat);
        }
    }

    /**
     * Retorna os totais calculados a partir das refeições (somente leitura).
     * Se não houver refeições ou itens, retorna os macros armazenados (modo MACRO_BUDGET).
     */
    public Macronutrients calculateTotalsFromMeals() {
        if (meals.isEmpty()) {
            return macros;
        }
        double calories = 0;
        double protein = 0;
        double carbohydrates = 0;
        double fat = 0;
        boolean hasAnyItems = false;
        for (Meal meal : meals) {
            Macronutrients totals = meal.calculateTotals();
            if (totals != null) {
                calories += totals.calories();
                protein += totals.protein();
                carbohydrates += totals.carbohydrates();
                fat += totals.fat();
                hasAnyItems = true;
            }
        }
        if (hasAnyItems) {
            return Macronutrients.of(calories, protein, carbohydrates, fat);
        }
        return macros;
    }

    /**
     * Gera warnings estruturados para esta dieta com base nos macros e perfil do atleta (T-087).
     * <p>
     * Os warnings são informativos — nunca bloqueiam a criação/aprovação.
     * A decisão final sempre pertence ao coach (regra inegociável).
     *
     * @param weightKg           peso do atleta em kg (da avaliação mais recente)
     * @param tmbKcal            taxa metabólica basal (kcal/dia)
     * @param getKcal            gasto energético total (kcal/dia)
     * @param balancePercent     balanço em % do GET
     * @param classification     classificação do balanço (DEFICIT, MAINTENANCE, SURPLUS)
     * @return lista de warnings (pode ser vazia)
     */
    public List<DietWarning> generateWarnings(double weightKg, double tmbKcal, double getKcal,
                                              double balancePercent, EnergyBalance.Classification classification) {
        List<DietWarning> result = new ArrayList<>();
        Macronutrients totals = calculateTotalsFromMeals();

        // Macros zerados
        if (totals.carbohydrates() == 0) {
            result.add(DietWarning.zeroCarbohydrate());
        }
        if (totals.fat() == 0) {
            result.add(DietWarning.zeroFat());
        }
        if (totals.protein() == 0) {
            result.add(DietWarning.zeroProtein());
        }

        // Consistência 4/4/9
        if (!totals.isConsistent()) {
            result.add(DietWarning.macroCalorieMismatch(totals.calories(), totals.calculatedCalories()));
        }

        // Calorias abaixo da TMB
        if (totals.calories() < tmbKcal) {
            result.add(DietWarning.lowCaloriesVsTmb(totals.calories(), tmbKcal));
        }

        // Déficit agressivo (> 25% do GET)
        if (classification == EnergyBalance.Classification.DEFICIT && balancePercent < -25) {
            result.add(DietWarning.aggressiveDeficit(balancePercent));
        }

        // Proteína abaixo de 1,6 g/kg
        double proteinGPerKg = totals.proteinGPerKg(weightKg);
        if (proteinGPerKg > 0 && proteinGPerKg < 1.6) {
            result.add(DietWarning.proteinBelowRange(proteinGPerKg));
        }

        // Gordura abaixo de 0,5 g/kg
        double fatGPerKg = totals.fatGPerKg(weightKg);
        if (fatGPerKg > 0 && fatGPerKg < 0.5) {
            result.add(DietWarning.fatBelowRange(fatGPerKg));
        }

        // Atualiza warnings internos
        this.warnings.clear();
        this.warnings.addAll(result);
        return result;
    }

    /**
     * Retorna os warnings atuais da dieta (somente leitura).
     */
    public List<DietWarning> warnings() {
        return Collections.unmodifiableList(warnings);
    }

    /**
     * Retorna os macros derivados para resposta da API (T-087).
     * Inclui: g/kg, %kcal, kcal/kg, consistência 4/4/9.
     *
     * @param weightKg peso do atleta em kg
     * @return objeto com valores derivados
     */
    public DerivedMacros derivedMacros(double weightKg) {
        Macronutrients totals = calculateTotalsFromMeals();
        return new DerivedMacros(
                totals.proteinGPerKg(weightKg),
                totals.carbohydratesGPerKg(weightKg),
                totals.fatGPerKg(weightKg),
                totals.proteinPctKcal(),
                totals.carbohydratesPctKcal(),
                totals.fatPctKcal(),
                totals.kcalPerKg(weightKg),
                totals.calculatedCalories(),
                totals.calories(),
                totals.consistencyDeltaKcal(),
                totals.consistencyDeltaPercent(),
                totals.isConsistent()
        );
    }

    /**
     * Valores derivados dos macros para exibição (T-087).
     */
    public record DerivedMacros(
            double proteinGPerKg,
            double carbohydratesGPerKg,
            double fatGPerKg,
            double proteinPctKcal,
            double carbohydratesPctKcal,
            double fatPctKcal,
            double kcalPerKg,
            double calculatedCalories,
            double declaredCalories,
            double consistencyDeltaKcal,
            double consistencyDeltaPercent,
            boolean isConsistent
    ) {
    }

    /**
     * Transiciona o estado da dieta seguindo o fluxo
     * Rascunho -> Em Revisão -> Aprovada -> Arquivada (US-017).
     * Transições inválidas são bloqueadas.
     */
    public void changeStatus(DietStatus newStatus) {
        if (deleted) {
            throw new IllegalStateException("Cannot change status of a deleted diet");
        }
        if (readOnly) {
            throw new IllegalStateException("Cannot change status of a read-only version");
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("newStatus must not be null");
        }
        if (!isValidTransition(status, newStatus)) {
            throw new IllegalStateException("Invalid status transition: " + status + " -> " + newStatus);
        }
        DietStatus oldStatus = this.status;
        this.status = newStatus;
        this.updatedAt = Instant.now();
        registerEvent(new DietStatusChanged(id, oldStatus, newStatus, updatedAt));
    }

    /**
     * Exclusão lógica (soft-delete). Nunca remove fisicamente.
     */
    public void softDelete(UUID deletedBy) {
        if (deleted) {
            return;
        }
        this.deleted = true;
        this.updatedBy = deletedBy;
        this.updatedAt = Instant.now();
        registerEvent(new DietDeleted(id, deletedBy, updatedAt));
    }

    private static boolean isValidTransition(DietStatus from, DietStatus to) {
        return switch (from) {
            case DRAFT -> to == DietStatus.IN_REVIEW;
            case IN_REVIEW -> to == DietStatus.APPROVED;
            case APPROVED -> to == DietStatus.ARCHIVED;
            case ARCHIVED -> false;
        };
    }

    public List<DietEvent> domainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }

    private void registerEvent(DietEvent event) {
        domainEvents.add(event);
    }

    public DietId id() {
        return id;
    }

    public CoachId coachId() {
        return coachId;
    }

    public AthleteId athleteId() {
        return athleteId;
    }

    public DietObjective objective() {
        return objective;
    }

    public Macronutrients macros() {
        return macros;
    }

    public DietStatus status() {
        return status;
    }

    public int version() {
        return version;
    }

    public DietId previousVersionId() {
        return previousVersionId;
    }

    public String changeReason() {
        return changeReason;
    }

    public UUID createdBy() {
        return createdBy;
    }

    public UUID updatedBy() {
        return updatedBy;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public boolean isDeleted() {
        return deleted;
    }

    /**
     * Retorna as refeições da dieta (somente leitura).
     */
    public List<Meal> meals() {
        return Collections.unmodifiableList(meals);
    }
}