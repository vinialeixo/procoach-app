package com.fitness.procoach.nutrition.domain;

/**
 * Macronutrientes de um alimento por 100 g (T-086).
 *
 * <p>Diferente de {@link Macronutrients} (metas de dieta, sempre positivas), um
 * alimento pode ter carboidratos ou gorduras zerados (ex.: peito de frango sem
 * pele). Por isso todos os valores aceitam {@code >= 0}.</p>
 */
public record FoodMacros(double calories, double protein, double carbohydrates, double fat) {

    public FoodMacros {
        if (calories < 0) {
            throw new IllegalArgumentException("calories must be >= 0");
        }
        if (protein < 0) {
            throw new IllegalArgumentException("protein must be >= 0");
        }
        if (carbohydrates < 0) {
            throw new IllegalArgumentException("carbohydrates must be >= 0");
        }
        if (fat < 0) {
            throw new IllegalArgumentException("fat must be >= 0");
        }
    }

    public static FoodMacros of(double calories, double protein, double carbohydrates, double fat) {
        return new FoodMacros(calories, protein, carbohydrates, fat);
    }
}
