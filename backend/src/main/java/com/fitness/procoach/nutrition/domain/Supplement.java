package com.fitness.procoach.nutrition.domain;

import java.util.Objects;

/**
 * Suplemento na dieta (T-090).
 *
 * <p>Regras inegociáveis aplicadas aqui:
 * <ul>
 *   <li>Suplementos não calóricos (creatina, multivitamínico, etc.) ficam na seção {@code supplements[]}
 *       fora das refeições, com {@code countsTowardMacros = false}.</li>
 *   <li>Suplementos calóricos (ex.: Whey Protein) entram como {@link MealItem} com
 *       {@link FoodKind#SUPPLEMENT_CALORIC} e {@code countsTowardMacros = true}.</li>
 * </ul>
 *
 * <p>Framework-agnostic: nenhuma anotação Spring/JPA.
 */
public class Supplement {

    private final String name;
    private final String dose;
    private final String timing;
    private final boolean countsTowardMacros;

    private Supplement(String name, String dose, String timing, boolean countsTowardMacros) {
        this.name = Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        this.dose = Objects.requireNonNull(dose, "dose");
        this.timing = timing; // opcional
        this.countsTowardMacros = countsTowardMacros;
    }

    /**
     * Cria um novo suplemento.
     *
     * @param name               nome do suplemento (ex.: "Creatina monoidratada", "Whey Protein")
     * @param dose               dose (ex.: "5 g", "30 g")
     * @param timing             horário de consumo (ex.: "qualquer horário", "pós-treino"), pode ser null
     * @param countsTowardMacros se true, o suplemento conta para os macros (é calórico, como Whey);
     *                           se false, não conta (creatina, multivitamínico, etc.)
     */
    public static Supplement create(String name, String dose, String timing, boolean countsTowardMacros) {
        return new Supplement(name, dose, timing, countsTowardMacros);
    }

    public String name() {
        return name;
    }

    public String dose() {
        return dose;
    }

    public String timing() {
        return timing;
    }

    public boolean countsTowardMacros() {
        return countsTowardMacros;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Supplement that)) return false;
        return countsTowardMacros == that.countsTowardMacros
                && name.equals(that.name)
                && Objects.equals(dose, that.dose)
                && Objects.equals(timing, that.timing);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, dose, timing, countsTowardMacros);
    }

    @Override
    public String toString() {
        return "Supplement[name=" + name + ", dose=" + dose + ", timing=" + timing
                + ", countsTowardMacros=" + countsTowardMacros + "]";
    }
}