package com.fitness.procoach.nutrition.domain;

import java.util.Objects;

/**
 * Macronutrientes de uma dieta (US-014): calorias, proteínas, carboidratos e gorduras.
 * Todos os valores devem ser positivos.
 */
public record Macronutrients(double calories, double protein, double carbohydrates, double fat) {

    public Macronutrients {
        if (calories <= 0) {
            throw new IllegalArgumentException("calories must be positive");
        }
        if (protein <= 0) {
            throw new IllegalArgumentException("protein must be positive");
        }
        if (carbohydrates <= 0) {
            throw new IllegalArgumentException("carbohydrates must be positive");
        }
        if (fat <= 0) {
            throw new IllegalArgumentException("fat must be positive");
        }
    }

    public static Macronutrients of(double calories, double protein, double carbohydrates, double fat) {
        return new Macronutrients(calories, protein, carbohydrates, fat);
    }

    @Override
    public String toString() {
        return "Macronutrients[calories=" + calories + ", protein=" + protein
                + ", carbohydrates=" + carbohydrates + ", fat=" + fat + "]";
    }
}
