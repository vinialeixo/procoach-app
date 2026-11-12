package com.fitness.procoach.nutrition.infrastructure;

import com.fitness.procoach.nutrition.domain.Macronutrients;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidade JPA do Meal (refeição).
 * Mapeada para a tabela {@code meals} criada pela migração Flyway.
 */
@Entity
@Table(name = "meals")
public class MealJpaEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diet_id", nullable = false)
    private DietJpaEntity diet;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int orderIndex;

    @Column(name = "time")
    private String time;

    @Column(name = "notes")
    private String notes;

    @Column(name = "target_calories")
    private Double targetCalories;

    @Column(name = "target_protein")
    private Double targetProtein;

    @Column(name = "target_carbohydrates")
    private Double targetCarbohydrates;

    @Column(name = "target_fat")
    private Double targetFat;

    @OneToMany(mappedBy = "meal", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id")
    private List<MealItemJpaEntity> items = new ArrayList<>();

    protected MealJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public DietJpaEntity getDiet() {
        return diet;
    }

    public void setDiet(DietJpaEntity diet) {
        this.diet = diet;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Double getTargetCalories() {
        return targetCalories;
    }

    public void setTargetCalories(Double targetCalories) {
        this.targetCalories = targetCalories;
    }

    public Double getTargetProtein() {
        return targetProtein;
    }

    public void setTargetProtein(Double targetProtein) {
        this.targetProtein = targetProtein;
    }

    public Double getTargetCarbohydrates() {
        return targetCarbohydrates;
    }

    public void setTargetCarbohydrates(Double targetCarbohydrates) {
        this.targetCarbohydrates = targetCarbohydrates;
    }

    public Double getTargetFat() {
        return targetFat;
    }

    public void setTargetFat(Double targetFat) {
        this.targetFat = targetFat;
    }

    public List<MealItemJpaEntity> getItems() {
        return items;
    }

    public void setItems(List<MealItemJpaEntity> items) {
        this.items = items;
    }

    public void addItem(MealItemJpaEntity item) {
        items.add(item);
        item.setMeal(this);
    }

    public void removeItem(MealItemJpaEntity item) {
        items.remove(item);
        item.setMeal(null);
    }
}