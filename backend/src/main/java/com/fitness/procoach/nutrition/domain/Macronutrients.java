package com.fitness.procoach.nutrition.domain;

import java.util.Objects;

/**
 * Macronutrientes de uma dieta (US-014): calorias, proteínas, carboidratos e gorduras.
 *
 * <p>Regras de validação (T-087):
 * <ul>
 *   <li>Calorias devem ser > 0</li>
 *   <li>Proteína deve ser > 0</li>
 *   <li>Carboidratos e gorduras podem ser >= 0 (estratégias cetogênicas, peak week)</li>
 *   <li>Consistência 4/4/9: calorias declaradas devem bater com 4*prot + 4*carb + 9*fat (±5% ou ±50 kcal)</li>
 * </ul>
 */
public record Macronutrients(double calories, double protein, double carbohydrates, double fat) {

    private static final double CALORIES_PER_GRAM_PROTEIN = 4.0;
    private static final double CALORIES_PER_GRAM_CARBOHYDRATE = 4.0;
    private static final double CALORIES_PER_GRAM_FAT = 9.0;
    private static final double CONSISTENCY_TOLERANCE_PERCENT = 0.05; // 5%
    private static final double CONSISTENCY_TOLERANCE_KCAL = 50.0;

    public Macronutrients {
        if (calories <= 0) {
            throw new IllegalArgumentException("calories must be positive");
        }
        if (protein <= 0) {
            throw new IllegalArgumentException("protein must be positive");
        }
        if (carbohydrates < 0) {
            throw new IllegalArgumentException("carbohydrates must be >= 0");
        }
        if (fat < 0) {
            throw new IllegalArgumentException("fat must be >= 0");
        }
    }

    public static Macronutrients of(double calories, double protein, double carbohydrates, double fat) {
        return new Macronutrients(calories, protein, carbohydrates, fat);
    }

    /**
     * Calcula as calorias teóricas baseadas em 4/4/9.
     */
    public double calculatedCalories() {
        return protein * CALORIES_PER_GRAM_PROTEIN
                + carbohydrates * CALORIES_PER_GRAM_CARBOHYDRATE
                + fat * CALORIES_PER_GRAM_FAT;
    }

    /**
     * Verifica se as calorias declaradas são consistentes com os macros (4/4/9).
     * Tolerância: ±5% ou ±50 kcal, o que for maior.
     */
    public boolean isConsistent() {
        double calculated = calculatedCalories();
        double delta = Math.abs(calories - calculated);
        double tolerance = Math.max(calories * CONSISTENCY_TOLERANCE_PERCENT, CONSISTENCY_TOLERANCE_KCAL);
        return delta <= tolerance;
    }

    /**
     * Retorna o delta entre calorias declaradas e calculadas (positivo = declaradas > calculadas).
     */
    public double consistencyDeltaKcal() {
        return calories - calculatedCalories();
    }

    /**
     * Retorna o delta percentual entre calorias declaradas e calculadas.
     */
    public double consistencyDeltaPercent() {
        double calculated = calculatedCalories();
        if (calculated == 0) return 0;
        return (calories - calculated) / calculated * 100;
    }

    /**
     * Percentual de calorias vindas da proteína.
     */
    public double proteinPctKcal() {
        double calc = calculatedCalories();
        if (calc == 0) return 0;
        return protein * CALORIES_PER_GRAM_PROTEIN / calc * 100;
    }

    /**
     * Percentual de calorias vindas dos carboidratos.
     */
    public double carbohydratesPctKcal() {
        double calc = calculatedCalories();
        if (calc == 0) return 0;
        return carbohydrates * CALORIES_PER_GRAM_CARBOHYDRATE / calc * 100;
    }

    /**
     * Percentual de calorias vindas das gorduras.
     */
    public double fatPctKcal() {
        double calc = calculatedCalories();
        if (calc == 0) return 0;
        return fat * CALORIES_PER_GRAM_FAT / calc * 100;
    }

    /**
     * Proteína em g/kg (requer peso do atleta em kg).
     */
    public double proteinGPerKg(double weightKg) {
        if (weightKg <= 0) return 0;
        return protein / weightKg;
    }

    /**
     * Carboidratos em g/kg (requer peso do atleta em kg).
     */
    public double carbohydratesGPerKg(double weightKg) {
        if (weightKg <= 0) return 0;
        return carbohydrates / weightKg;
    }

    /**
     * Gorduras em g/kg (requer peso do atleta em kg).
     */
    public double fatGPerKg(double weightKg) {
        if (weightKg <= 0) return 0;
        return fat / weightKg;
    }

    /**
     * Calorias por kg (requer peso do atleta em kg).
     */
    public double kcalPerKg(double weightKg) {
        if (weightKg <= 0) return 0;
        return calories / weightKg;
    }

    @Override
    public String toString() {
        return "Macronutrients[calories=" + calories + ", protein=" + protein
                + ", carbohydrates=" + carbohydrates + ", fat=" + fat + "]";
    }
}
