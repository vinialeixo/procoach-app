package com.fitness.procoach.nutrition.interfaces;

import com.fitness.procoach.nutrition.domain.DietObjective;
import com.fitness.procoach.nutrition.domain.FoodKind;
import com.fitness.procoach.nutrition.domain.FoodSource;
import com.fitness.procoach.nutrition.domain.FoodState;
import com.fitness.procoach.nutrition.domain.Macronutrients;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO para proposta de plano alimentar gerado por IA (T-088).
 * Contrato JSON v1 conforme {@code docs/nutricao-plano-alimentar.md §4}.
 */
@Schema(description = "Proposta de plano alimentar gerado por IA — contrato JSON v1. " +
        "O plano nasce como rascunho (DRAFT) e exige aprovação do coach (requiresCoachApproval=true). " +
        "Nunca é aprovado automaticamente. Auditoria: AI_PLAN_PROPOSED.")
public record AiPlanProposalRequest(
        @Schema(description = "Versão do contrato", example = "1.0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull String schemaVersion,

        @Schema(description = "Metadados da geração", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Valid Meta meta,

        @Schema(description = "Dados do atleta no momento da geração", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Valid AthleteSnapshot athlete,

        @Schema(description = "Estratégia nutricional", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Valid Strategy strategy,

        @Schema(description = "Metas de macronutrientes diárias", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Valid Targets targets,

        @Schema(description = "Macros derivados (g/kg, %kcal, kcal/kg, balanço)", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Valid Derived derived,

        @Schema(description = "Consistência calórica 4/4/9", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Valid Consistency consistency,

        @Schema(description = "Modo do plano: MACRO_BUDGET, MEAL_PLAN ou HYBRID", example = "MEAL_PLAN", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull String planMode,

        @Schema(description = "Refeições do plano", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Valid List<MealProposal> meals,

        @Schema(description = "Suplementos (não entram nos macros da refeição, exceto calóricos)")
        @Valid List<SupplementProposal> supplements,

        @Schema(description = "Warnings nutricionais estruturados", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Valid List<WarningProposal> warnings,

        @Schema(description = "Totais calculados", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Valid Totals totals,

        @Schema(description = "Disclaimer padrão", example = "Plano consultivo. A decisão final e a aprovação são do coach.")
        String disclaimer
) {

    @Schema(description = "Metadados da geração pela IA")
    public record Meta(
            @Schema(description = "Timestamp da geração ISO-8601", example = "2026-11-11T12:00:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull String generatedAt,

            @Schema(description = "Origem da proposta", example = "AI_ASSISTANT", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull String source,

            @Schema(description = "Sempre true — regra inegociável: coach deve aprovar", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Boolean requiresCoachApproval,

            @Schema(description = "ID da versão base para criar nova versão (null para primeira dieta)")
            UUID baseDietVersionId
    ) {}

    @Schema(description = "Snapshot do atleta no momento da geração")
    public record AthleteSnapshot(
            @Schema(description = "ID do atleta", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull UUID athleteId,

            @Schema(description = "Nome do atleta", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull String name,

            @Schema(description = "Sexo", example = "MALE", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull String sex,

            @Schema(description = "Idade em anos", example = "25", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Integer age,

            @Schema(description = "Altura em cm", example = "175", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Integer heightCm,

            @Schema(description = "Peso em kg", example = "75.0", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Double weightKg,

            @Schema(description = "Percentual de gordura corporal", example = "12.0")
            Double bodyFatPercent
    ) {}

    @Schema(description = "Estratégia nutricional")
    public record Strategy(
            @Schema(description = "Objetivo da dieta", example = "CUTTING", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull DietObjective objective,

            @Schema(description = "Fase do protocolo", example = "PRE_CONTEST")
            String phase,

            @Schema(description = "Se é dia de treino", example = "true")
            Boolean trainingDay,

            @Schema(description = "Notas da estratégia")
            String notes
    ) {}

    @Schema(description = "Metas de macronutrientes")
    public record Targets(
            @Schema(description = "Calorias diárias (kcal)", example = "2500", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Double calories,

            @Schema(description = "Proteína (g)", example = "180", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Double protein,

            @Schema(description = "Carboidratos (g) — zero permitido", example = "250", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Double carbohydrates,

            @Schema(description = "Gorduras (g) — zero permitido", example = "80", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Double fat,

            @Schema(description = "Fibras (g)", example = "30")
            Double fiber,

            @Schema(description = "Água (ml)", example = "3500")
            Double waterMl
    ) {}

    @Schema(description = "Valores derivados para exibição")
    public record Derived(
            @Schema(description = "TMB (kcal/dia)", example = "1800", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Double tmbKcal,

            @Schema(description = "GET (kcal/dia)", example = "2400", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Double getKcal,

            @Schema(description = "Balanço calórico vs GET", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Double balanceKcal,

            @Schema(description = "Balanço em % do GET", example = "4.2", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Double balancePercent,

            @Schema(description = "Classificação do balanço", example = "SURPLUS", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull String classification,

            @Schema(description = "Proteína g/kg", example = "2.4", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Double proteinGPerKg,

            @Schema(description = "Carboidratos g/kg", example = "3.3", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Double carbohydratesGPerKg,

            @Schema(description = "Gorduras g/kg", example = "1.1", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Double fatGPerKg,

            @Schema(description = "Proteína %kcal", example = "28.8", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Double proteinPctKcal,

            @Schema(description = "Carboidratos %kcal", example = "40.0", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Double carbohydratesPctKcal,

            @Schema(description = "Gorduras %kcal", example = "28.8", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Double fatPctKcal
    ) {}

    @Schema(description = "Consistência 4/4/9")
    public record Consistency(
            @Schema(description = "Calorias calculadas (4/4/9)", example = "2500", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Double calculatedCalories,

            @Schema(description = "Calorias declaradas", example = "2500", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Double declaredCalories,

            @Schema(description = "Delta em kcal", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Double deltaKcal,

            @Schema(description = "Delta em %", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Double deltaPercent,

            @Schema(description = "Se está consistente (±5% ou ±50 kcal)", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Boolean isConsistent
    ) {}

    @Schema(description = "Refeição proposta")
    public record MealProposal(
            @Schema(description = "ID temporário da refeição", example = "meal-1", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull String mealId,

            @Schema(description = "Nome da refeição", example = "Café da manhã", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull String name,

            @Schema(description = "Ordem da refeição no dia", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Integer order,

            @Schema(description = "Horário sugerido", example = "07:00", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull String time,

            @Schema(description = "Notas da refeição")
            String notes,

            @Schema(description = "Metas de macros para esta refeição")
            @Valid MealTargetProposal target,

            @Schema(description = "Itens da refeição", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull @Valid List<ItemProposal> items,

            @Schema(description = "Totais calculados da refeição", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull @Valid MacronutrientsProposal totals
    ) {}

    @Schema(description = "Metas de macros por refeição")
    public record MealTargetProposal(
            @Schema(description = "Calorias", example = "600", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Double calories,

            @Schema(description = "Proteína (g)", example = "40", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Double protein,

            @Schema(description = "Carboidratos (g)", example = "60", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Double carbohydrates,

            @Schema(description = "Gorduras (g)", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Double fat
    ) {}

    @Schema(description = "Item de refeição")
    public record ItemProposal(
            @Schema(description = "ID do alimento no catálogo (ex.: taco:ovo-de-galinha-inteiro)", example = "taco:ovo-de-galinha-inteiro", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull String foodId,

            @Schema(description = "Nome do alimento", example = "Ovo, de galinha, inteiro", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull String name,

            @Schema(description = "Origem do alimento", example = "TACO", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull FoodSource source,

            @Schema(description = "Tipo: FOOD ou SUPPLEMENT_CALORIC", example = "FOOD", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull FoodKind kind,

            @Schema(description = "Estado: RAW ou AS_SERVED", example = "RAW", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull FoodState state,

            @Schema(description = "Quantidade em gramas (peso cru)", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Double quantityG,

            @Schema(description = "Medida caseira", example = "2 unidades")
            String householdMeasure,

            @Schema(description = "Modo de preparo (informativo)")
            String preparation,

            @Schema(description = "Macros do item (snapshot no momento da prescrição)", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull @Valid MacronutrientsProposal macros
    ) {}

    @Schema(description = "Suplemento proposto")
    public record SupplementProposal(
            @Schema(description = "Nome do suplemento", example = "Creatina monoidratada", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull String name,

            @Schema(description = "Dose", example = "5 g", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull String dose,

            @Schema(description = "Horário sugerido", example = "qualquer horário")
            String timing,

            @Schema(description = "Se conta nos macros da dieta", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Boolean countsTowardMacros
    ) {}

    @Schema(description = "Warning nutricional estruturado")
    public record WarningProposal(
            @Schema(description = "Código do warning", example = "ZERO_CARBOHYDRATE", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull String code,

            @Schema(description = "Severidade: INFO, WARNING, CRITICAL", example = "WARNING", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull String severity,

            @Schema(description = "Mensagem explicativa", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull String message
    ) {}

    @Schema(description = "Totais do dia")
    public record Totals(
            @Schema(description = "Calorias totais", example = "2500", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Double calories,

            @Schema(description = "Proteína total (g)", example = "180", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Double protein,

            @Schema(description = "Carboidratos totais (g)", example = "250", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Double carbohydrates,

            @Schema(description = "Gorduras totais (g)", example = "80", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Double fat
    ) {}

    @Schema(description = "Macronutrientes (para itens e totais)")
    public record MacronutrientsProposal(
            @Schema(description = "Calorias", example = "143", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Double calories,

            @Schema(description = "Proteína (g)", example = "13.0", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Double protein,

            @Schema(description = "Carboidratos (g)", example = "1.6", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Double carbohydrates,

            @Schema(description = "Gorduras (g)", example = "9.5", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Double fat
    ) {}
}