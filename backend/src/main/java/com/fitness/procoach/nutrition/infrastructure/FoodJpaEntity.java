package com.fitness.procoach.nutrition.infrastructure;

import com.fitness.procoach.nutrition.domain.FoodSource;
import com.fitness.procoach.nutrition.domain.FoodState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Entidade JPA do catálogo de alimentos. Mapeada para a tabela {@code foods}
 * criada pela migração Flyway V402.
 */
@Entity
@Table(name = "foods")
public class FoodJpaEntity {

	@Id
	private UUID id;

	@Column(name = "taco_id", unique = true)
	private String tacoId;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(length = 100)
	private String category;

	@Column(nullable = false)
	private double calories;

	@Column(nullable = false)
	private double protein;

	@Column(nullable = false)
	private double carbohydrates;

	@Column(nullable = false)
	private double fat;

	@Column(name = "edible_portion_percent", nullable = false)
	private double ediblePortionPercent;

	@Column(name = "household_measure", length = 100)
	private String householdMeasure;

	@Column(name = "household_measure_grams")
	private Double householdMeasureGrams;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private FoodSource source;

	@Enumerated(EnumType.STRING)
	@Column(name = "state_default", nullable = false, length = 16)
	private FoodState stateDefault;

	@Column(name = "coach_id")
	private UUID coachId;

	@Column(name = "coach_approved", nullable = false)
	private boolean coachApproved;

	@Column(nullable = false)
	private boolean deleted;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected FoodJpaEntity() {
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getTacoId() {
		return tacoId;
	}

	public void setTacoId(String tacoId) {
		this.tacoId = tacoId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
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

	public double getEdiblePortionPercent() {
		return ediblePortionPercent;
	}

	public void setEdiblePortionPercent(double ediblePortionPercent) {
		this.ediblePortionPercent = ediblePortionPercent;
	}

	public String getHouseholdMeasure() {
		return householdMeasure;
	}

	public void setHouseholdMeasure(String householdMeasure) {
		this.householdMeasure = householdMeasure;
	}

	public Double getHouseholdMeasureGrams() {
		return householdMeasureGrams;
	}

	public void setHouseholdMeasureGrams(Double householdMeasureGrams) {
		this.householdMeasureGrams = householdMeasureGrams;
	}

	public FoodSource getSource() {
		return source;
	}

	public void setSource(FoodSource source) {
		this.source = source;
	}

	public FoodState getStateDefault() {
		return stateDefault;
	}

	public void setStateDefault(FoodState stateDefault) {
		this.stateDefault = stateDefault;
	}

	public UUID getCoachId() {
		return coachId;
	}

	public void setCoachId(UUID coachId) {
		this.coachId = coachId;
	}

	public boolean isCoachApproved() {
		return coachApproved;
	}

	public void setCoachApproved(boolean coachApproved) {
		this.coachApproved = coachApproved;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}
