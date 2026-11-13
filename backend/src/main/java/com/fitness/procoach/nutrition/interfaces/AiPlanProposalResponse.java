package com.fitness.procoach.nutrition.interfaces;

import com.fitness.procoach.nutrition.domain.Diet;
import com.fitness.procoach.nutrition.domain.DietObjective;
import com.fitness.procoach.nutrition.domain.DietStatus;
import com.fitness.procoach.nutrition.domain.Macronutrients;
import com.fitness.procoach.nutrition.domain.Meal;
import com.fitness.procoach.nutrition.domain.MealItem;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO para proposta de plano alimentar gerado por IA (T-088).
 * Retorna a dieta criada em DRAFT (ou validação enriquecida no dry-run).
 */
@Schema(description = "Resposta da proposta de plano por IA. Contém a dieta criada em DRAFT " +
        "(requiresCoachApproval=true) ou a validação enriquecida (dry-run).")
public record AiPlanProposalResponse(
        @Schema(description = "Identificador da dieta criada", example = "5d4c3b2a-1f0e-4d5c-8b7a-6f5e4d3c2b1a")
        UUID dietId,

        @Schema(description = "Versão da dieta", example = "1")
        Integer version,

        @Schema(description = "Status da dieta (sempre DRAFT para propostas de IA)", example = "DRAFT")
        DietStatus status,

        @Schema(description = "Se exige aprovação do coach (sempre true)", example = "true")
        Boolean requiresCoachApproval,

        @Schema(description = "Objetivo da dieta", example = "CUTTING")
        DietObjective objective,

        @Schema(description = "Calorias diárias", example = "2500.0")
        Double calories,

        @Schema(description = "Proteína diária (g)", example = "180.0")
        Double protein,

        @Schema(description = "Carboidratos diários (g)", example = "250.0")
        Double carbohydrates,

        @Schema(description = "Gorduras diárias (g)", example = "80.0")
        Double fat,

        @Schema(description = "Refeições criadas")
        List<MealResponse> meals,

        @Schema(description = "Totais calculados")
        DietTotalsResponse totals,

        @Schema(description = "Warnings nutricionais gerados/validados")
        List<WarningResponse> warnings,

        @Schema(description = "Macros derivados (g/kg, %kcal, consistência)")
        DerivedMacrosResponse derived,

        @Schema(description = "Disclaimer", example = "Plano consultivo. A decisão final e a aprovação são do coach.")
        String disclaimer,

        @Schema(description = "ID da versão anterior (se criou nova versão)")
        UUID previousVersionId
) {

    public static AiPlanProposalResponse from(Diet diet, double weightKg, Boolean requiresCoachApproval, UUID previousVersionId) {
        List<MealResponse> meals = diet.meals().stream().map(AiPlanProposalResponse::mapMeal).toList();
        Macronutrients totals = diet.calculateTotalsFromMeals();
        DietTotalsResponse totalsResponse = new DietTotalsResponse(totals.calories(), totals.protein(),
                totals.carbohydrates(), totals.fat());

        List<WarningResponse> warnings = diet.warnings().stream()
                .map(WarningResponse::from)
                .toList();

        DerivedMacrosResponse derived = null;
        if (weightKg > 0) {
            Diet.DerivedMacros dm = diet.derivedMacros(weightKg);
            derived = new DerivedMacrosResponse(
                    dm.proteinGPerKg(), dm.carbohydratesGPerKg(), dm.fatGPerKg(),
                    dm.proteinPctKcal(), dm.carbohydratesPctKcal(), dm.fatPctKcal(),
                    dm.kcalPerKg(), dm.calculatedCalories(), dm.declaredCalories(),
                    dm.consistencyDeltaKcal(), dm.consistencyDeltaPercent(), dm.isConsistent());
        }

        return new AiPlanProposalResponse(
                diet.id().value(),
                diet.version(),
                diet.status(),
                requiresCoachApproval,
                diet.objective(),
                diet.macros().calories(),
                diet.macros().protein(),
                diet.macros().carbohydrates(),
                diet.macros().fat(),
                meals,
                totalsResponse,
                warnings,
                derived,
                "Plano consultivo. A decisão final e a aprovação são do coach.",
                previousVersionId
        );
    }

    private static MealResponse mapMeal(Meal meal) {
        List<MealItemResponse> items = meal.items().stream().map(AiPlanProposalResponse::mapItem).toList();
        MealTargetResponse target = meal.target() != null ? new MealTargetResponse(
                meal.target().calories(), meal.target().protein(), meal.target().carbohydrates(), meal.target().fat()) : null;
        Macronutrients totals = meal.calculateTotals();
        MealTotalsResponse totalsResponse = totals != null ? new MealTotalsResponse(totals.calories(), totals.protein(),
                totals.carbohydrates(), totals.fat()) : null;
        return new MealResponse(meal.id().value(), meal.name(), meal.order(), meal.time(),
                meal.notes(), target, items, totalsResponse);
    }

    private static MealItemResponse mapItem(MealItem item) {
        return new MealItemResponse(item.id().value(), item.foodId(), item.name(), item.source(),
                item.kind(), item.state(), item.quantityG(), item.householdMeasure(),
                item.preparation(), item.macrosSnapshot().calories(), item.macrosSnapshot().protein(),
                item.macrosSnapshot().carbohydrates(), item.macrosSnapshot().fat());
    }

    public static AiPlanProposalResponse fromValidation(com.fitness.procoach.nutrition.domain.AiPlanProposal proposal, double weightKg) {
        // Para dry-run, não há Diet persistida, então construímos a resposta a partir da proposta enriquecida
        return new AiPlanProposalResponse(
                null,
                1,
                DietStatus.DRAFT,
                true,
                proposal.objective(),
                proposal.targets().calories(),
                proposal.targets().protein(),
                proposal.targets().carbohydrates(),
                proposal.targets().fat(),
                List.of(), // meals não convertidas para response no dry-run
                null,
                proposal.warnings().stream().map(WarningResponse::from).toList(),
                proposal.derived() != null ? new DerivedMacrosResponse(
                        proposal.derived().proteinGPerKg(),
                        proposal.derived().carbohydratesGPerKg(),
                        proposal.derived().fatGPerKg(),
                        proposal.derived().proteinPctKcal(),
                        proposal.derived().carbohydratesPctKcal(),
                        proposal.derived().fatPctKcal(),
                        0, // kcalPerKg não disponível na proposta
                        proposal.consistency().calculatedCalories(),
                        proposal.consistency().declaredCalories(),
                        proposal.consistency().deltaKcal(),
                        proposal.consistency().deltaPercent(),
                        proposal.consistency().isConsistent()
                ) : null,
                proposal.disclaimer(),
                proposal.baseDietVersionId() != null ? proposal.baseDietVersionId().value() : null
        );
    }
}