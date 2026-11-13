package com.fitness.procoach.nutrition.interfaces;

import com.fitness.procoach.nutrition.domain.Diet;
import com.fitness.procoach.nutrition.domain.DietObjective;
import com.fitness.procoach.nutrition.domain.DietStatus;
import com.fitness.procoach.nutrition.domain.EnergyBalance;
import com.fitness.procoach.nutrition.domain.Macronutrients;
import com.fitness.procoach.nutrition.domain.Meal;
import com.fitness.procoach.nutrition.domain.MealItem;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO de saída de uma dieta.
 */
@Schema(description = "Versão de uma dieta. Regra inegociável (US-015): uma dieta NUNCA é sobrescrita — toda "
        + "alteração gera uma nova versão encadeada (v1→v2→v3) e as versões anteriores tornam-se somente "
        + "leitura (readOnly=true). A partir do T-085, inclui refeições e itens com totais derivados. "
        + "A partir do T-087, inclui warnings estruturados e macros derivados (g/kg, %kcal, consistência 4/4/9). "
        + "A partir do T-089, inclui balanço energético (déficit/manutenção/superávit vs GET).")
public record DietResponse(
        @Schema(description = "Identificador desta versão da dieta",
                example = "5d4c3b2a-1f0e-4d5c-8b7a-6f5e4d3c2b1a") UUID id,
        @Schema(description = "Identificador do coach responsável",
                example = "2f6b1c3d-7a0e-4b5d-9c8a-1e2f3a4b5c6d") UUID coachId,
        @Schema(description = "Identificador do atleta",
                example = "8c1d2e3f-4a5b-4c6d-8e9f-0a1b2c3d4e5f") UUID athleteId,
        @Schema(description = "Objetivo da dieta", example = "HYPERTROPHY") DietObjective objective,
        @Schema(description = "Calorias diárias totais (kcal) — snapshot dos totais (metas ou derivados dos itens)", example = "3200.0") double calories,
        @Schema(description = "Proteína diária em gramas — snapshot dos totais", example = "180.0") double protein,
        @Schema(description = "Carboidratos diários em gramas — snapshot dos totais", example = "350.0") double carbohydrates,
        @Schema(description = "Gorduras diárias em gramas — snapshot dos totais", example = "90.0") double fat,
        @Schema(description = "Estado da dieta (Rascunho, Em Revisão, Aprovada, Arquivada)",
                example = "DRAFT") DietStatus status,
        @Schema(description = "Número desta versão na cadeia de versionamento (v1, v2, ...)", example = "1") int version,
        @Schema(description = "Identificador da versão anterior na cadeia (null na v1) — evidência de que a "
                + "dieta nunca é sobrescrita (US-015)",
                example = "5d4c3b2a-1f0e-4d5c-8b7a-6f5e4d3c2b1a") UUID previousVersionId,
        @Schema(description = "Motivo da alteração registrado na criação desta versão",
                example = "Ajuste de calorias após reavaliação") String changeReason,
        @Schema(description = "Data/hora de criação do registro", example = "2026-09-01T10:15:30Z") Instant createdAt,
        @Schema(description = "Data/hora da última atualização", example = "2026-09-01T10:15:30Z") Instant updatedAt,
        @Schema(description = "true quando esta versão foi substituída por uma mais recente e tornou-se somente "
                + "leitura — nunca pode ser alterada ou sobrescrita (US-015)", example = "false") boolean readOnly,
        @Schema(description = "true quando o registro foi excluído logicamente (soft-delete)",
                example = "false") boolean deleted,
        @Schema(description = "Usuário que criou esta versão (auditoria, US-016)",
                example = "2f6b1c3d-7a0e-4b5d-9c8a-1e2f3a4b5c6d") UUID createdBy,
        @Schema(description = "Último usuário que alterou o registro",
                example = "2f6b1c3d-7a0e-4b5d-9c8a-1e2f3a4b5c6d") UUID updatedBy,
        @Schema(description = "Refeições da dieta (T-085). Totais derivados dos itens.") List<MealResponse> meals,
        @Schema(description = "Totais calculados a partir das refeições (somente leitura). Se não houver refeições, igual aos macros informados.") DietTotalsResponse totals,
        @Schema(description = "Warnings nutricionais estruturados (T-087). Informativos — nunca bloqueiam.") List<WarningResponse> warnings,
        @Schema(description = "Macros derivados: g/kg, %kcal, kcal/kg, consistência 4/4/9 (T-087).") DerivedMacrosResponse derived,
        @Schema(description = "Balanço energético vs GET da avaliação mais recente (T-089). Null se atleta não tem avaliação.") EnergyBalanceResponse energyBalance) {

    public static DietResponse from(Diet diet, double weightKg) {
        return from(diet, weightKg, null);
    }

    public static DietResponse from(Diet diet, double weightKg, EnergyBalance balance) {
        List<MealResponse> meals = diet.meals().stream().map(DietResponse::mapMeal).toList();
        Macronutrients totals = diet.calculateTotalsFromMeals();
        DietTotalsResponse totalsResponse = new DietTotalsResponse(totals.calories(), totals.protein(),
                totals.carbohydrates(), totals.fat());
        
        // Generate warnings and derived macros
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
        
        EnergyBalanceResponse energyBalanceResponse = null;
        if (balance != null) {
            energyBalanceResponse = new EnergyBalanceResponse(
                    balance.getKcal(), balance.balanceKcal(), balance.balancePercent(), balance.classification().name());
        }
        
        return new DietResponse(diet.id().value(), diet.coachId().value(), diet.athleteId().value(),
                diet.objective(), diet.macros().calories(), diet.macros().protein(),
                diet.macros().carbohydrates(), diet.macros().fat(), diet.status(), diet.version(),
                diet.previousVersionId() == null ? null : diet.previousVersionId().value(),
                diet.changeReason(), diet.createdAt(), diet.updatedAt(), diet.isReadOnly(),
                diet.isDeleted(), diet.createdBy(), diet.updatedBy(), meals, totalsResponse,
                warnings, derived, energyBalanceResponse);
    }

    private static MealResponse mapMeal(Meal meal) {
        List<MealItemResponse> items = meal.items().stream().map(DietResponse::mapItem).toList();
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

    /**
     * Balanço energético simplificado para inclusão na DietResponse (T-089).
     */
    @Schema(description = "Balanço energético da dieta vs GET da avaliação mais recente.")
    public record EnergyBalanceResponse(
            @Schema(description = "Gasto energético total (kcal/dia)", example = "1602") BigDecimal getKcal,
            @Schema(description = "Calorias da dieta menos o GET (kcal/dia); negativo = déficit", example = "-202") BigDecimal balanceKcal,
            @Schema(description = "Balanço em % do GET", example = "-12.61") BigDecimal balancePercent,
            @Schema(description = "DEFICIT, MAINTENANCE (±5% do GET) ou SURPLUS", example = "DEFICIT") String classification) {
    }
}