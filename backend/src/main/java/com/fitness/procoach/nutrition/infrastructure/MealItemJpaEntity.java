package com.fitness.procoach.nutrition.infrastructure;

import com.fitness.procoach.nutrition.domain.FoodKind;
import com.fitness.procoach.nutrition.domain.FoodSource;
import com.fitness.procoach.nutrition.domain.FoodState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Entidade JPA do MealItem (item de refeição).
 * Mapeada para a tabela {@code meal_items} criada pela migração Flyway.
 */
@Entity
@Table(name = "meal_items")
public class MealItemJpaEntity {

    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "meal_id", nullable = false)
    private MealJpaEntity meal;

    @Column(name = "food_id")
    private String foodId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FoodSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FoodKind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FoodState state;

    @Column(name = "quantity_g", nullable = false)
    private double quantityG;

    @Column(name = "household_measure")
    private String householdMeasure;

    @Column(name = "preparation")
    private String preparation;

    @Column(name = "calories", nullable = false)
    private double calories;

    @Column(name = "protein", nullable = false)
    private double protein;

    @Column(name = "carbohydrates", nullable = false)
    private double carbohydrates;

    @Column(name = "fat", nullable = false)
    private double fat;

    protected MealItemJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public MealJpaEntity getMeal() {
        return meal;
    }

    public void setMeal(MealJpaEntity meal) {
        this.meal = meal;
    }

    public String getFoodId() {
        return foodId;
    }

    public void setFoodId(String foodId) {
        this.foodId = foodId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FoodSource getSource() {
        return source;
    }

    public void setSource(FoodSource source) {
        this.source = source;
    }

    public FoodKind getKind() {
        return kind;
    }

    public void setKind(FoodKind kind) {
        this.kind = kind;
    }

    public FoodState getState() {
        return state;
    }

    public void setState(FoodState state) {
        this.state = state;
    }

    public double getQuantityG() {
        return quantityG;
    }

    public void setQuantityG(double quantityG) {
        this.quantityG = quantityG;
    }

    public String getHouseholdMeasure() {
        return householdMeasure;
    }

    public void setHouseholdMeasure(String householdMeasure) {
        this.householdMeasure = householdMeasure;
    }

    public String getPreparation() {
        return preparation;
    }

    public void setPreparation(String preparation) {
        this.preparation = preparation;
    }

    public double getCalories() {
        return calories;
    }

    public void setCalories(double calories) {
        this.calories = calories;
    }

    public double getProtein() {
        return protein;
    }

    public void setProtein(double protein) {
        this.protein = protein;
    }

    public double getCarbohydrates() {
        return carbohydrates;
    }

    public void setCarbohydrates(double carbohydrates) {
        this.carbohydrates = carbohydrates;
    }

    public double getFat() {
        return fat;
    }

    public void setFat(double fat) {
        this.fat = fat;
    }
}