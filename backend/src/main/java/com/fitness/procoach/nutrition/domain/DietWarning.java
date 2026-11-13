package com.fitness.procoach.nutrition.domain;

import java.util.Objects;

/**
 * Aviso nutricional estruturado (T-087 / docs/nutricao-plano-alimentar.md §4.1).
 * <p>
 * Os warnings são informativos — nunca bloqueiam a criação/aprovação da dieta.
 * A decisão final sempre pertence ao coach (regra inegociável).
 */
public record DietWarning(
        /** Código do warning (ex.: ZERO_CARBOHYDRATE, MACRO_CALORIE_MISMATCH). */
        WarningCode code,
        /** Severidade: INFO, WARNING, CRITICAL. */
        Severity severity,
        /** Mensagem legível para o coach. */
        String message
) {

    public DietWarning {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(message, "message");
    }

    /**
     * Códigos de warning padronizados (T-087 / docs/nutricao-plano-alimentar.md §4.1).
     */
    public enum WarningCode {
        ZERO_CARBOHYDRATE,
        ZERO_FAT,
        ZERO_PROTEIN,
        MACRO_CALORIE_MISMATCH,
        LOW_CALORIES_VS_TMB,
        AGGRESSIVE_DEFICIT,
        PROTEIN_BELOW_RANGE,
        FAT_BELOW_RANGE,
        FIBER_BELOW_RECOMMENDATION,
        RESTRICTION_CONFLICT,
        RAW_WEIGHT_ASSUMED,
        SUPPLEMENT_EXCLUDED_FROM_MACROS
    }

    /**
     * Severidade do warning.
     */
    public enum Severity {
        INFO,
        WARNING,
        CRITICAL
    }

    /**
     * Cria warning para carboidrato zerado.
     */
    public static DietWarning zeroCarbohydrate() {
        return new DietWarning(
                WarningCode.ZERO_CARBOHYDRATE,
                Severity.WARNING,
                "Carboidrato zerado por estratégia (ex.: cetogênica, peak week); monitorar desempenho e hidratação."
        );
    }

    /**
     * Cria warning para gordura zerada.
     */
    public static DietWarning zeroFat() {
        return new DietWarning(
                WarningCode.ZERO_FAT,
                Severity.WARNING,
                "Gordura zerada; risco de deficiência de ácidos graxos essenciais e vitaminas lipossolúveis."
        );
    }

    /**
     * Cria warning para proteína zerada (crítico).
     */
    public static DietWarning zeroProtein() {
        return new DietWarning(
                WarningCode.ZERO_PROTEIN,
                Severity.CRITICAL,
                "Proteína zerada é incompatível com saúde e performance; revisar prescrição."
        );
    }

    /**
     * Cria warning para inconsistência calórica (4/4/9).
     */
    public static DietWarning macroCalorieMismatch(double declared, double calculated) {
        double delta = Math.abs(declared - calculated);
        double pct = calculated == 0 ? 0 : (delta / calculated) * 100;
        return new DietWarning(
                WarningCode.MACRO_CALORIE_MISMATCH,
                Severity.WARNING,
                String.format("Calorias declaradas (%.0f) divergem de 4/4/9 (%.0f): delta %.0f kcal (%.1f%%).",
                        declared, calculated, delta, pct)
        );
    }

    /**
     * Cria warning para calorias abaixo da TMB.
     */
    public static DietWarning lowCaloriesVsTmb(double calories, double tmbKcal) {
        return new DietWarning(
                WarningCode.LOW_CALORIES_VS_TMB,
                Severity.WARNING,
                String.format("Calorias da dieta (%.0f) abaixo da TMB (%.0f); risco de adaptação metabólica.", calories, tmbKcal)
        );
    }

    /**
     * Cria warning para déficit agressivo.
     */
    public static DietWarning aggressiveDeficit(double balancePercent) {
        return new DietWarning(
                WarningCode.AGGRESSIVE_DEFICIT,
                Severity.WARNING,
                String.format("Déficit agressivo (%.1f%% do GET); considerar abordagem mais conservadora.", balancePercent)
        );
    }

    /**
     * Cria warning para proteína abaixo da faixa recomendada (1,6 g/kg).
     */
    public static DietWarning proteinBelowRange(double proteinGPerKg) {
        return new DietWarning(
                WarningCode.PROTEIN_BELOW_RANGE,
                Severity.WARNING,
                String.format("Proteína (%.1f g/kg) abaixo da faixa recomendada (1,6–2,2 g/kg).", proteinGPerKg)
        );
    }

    /**
     * Cria warning para gordura abaixo da faixa recomendada (0,5 g/kg).
     */
    public static DietWarning fatBelowRange(double fatGPerKg) {
        return new DietWarning(
                WarningCode.FAT_BELOW_RANGE,
                Severity.WARNING,
                String.format("Gordura (%.1f g/kg) abaixo do mínimo recomendado (0,5 g/kg).", fatGPerKg)
        );
    }

    /**
     * Cria warning para fibras abaixo da recomendação.
     */
    public static DietWarning fiberBelowRecommendation(double fiberG) {
        return new DietWarning(
                WarningCode.FIBER_BELOW_RECOMMENDATION,
                Severity.INFO,
                String.format("Fibras (%.0f g) abaixo da recomendação (25–38 g/dia).", fiberG)
        );
    }
}