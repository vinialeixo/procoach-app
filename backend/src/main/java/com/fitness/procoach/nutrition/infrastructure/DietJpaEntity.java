package com.fitness.procoach.nutrition.infrastructure;

import com.fitness.procoach.nutrition.domain.DietObjective;
import com.fitness.procoach.nutrition.domain.DietStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidade JPA do agregado Diet. Mapeada para a tabela {@code diets} criada
 * pela migração Flyway V400.
 */
@Entity
@Table(name = "diets")
public class DietJpaEntity {

	@Id
	private UUID id;

	@Column(name = "coach_id", nullable = false)
	private UUID coachId;

	@Column(name = "athlete_id", nullable = false)
	private UUID athleteId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private DietObjective objective;

	@Column(nullable = false)
	private double calories;

	@Column(nullable = false)
	private double protein;

	@Column(nullable = false)
	private double carbohydrates;

	@Column(nullable = false)
	private double fat;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private DietStatus status;

	@Column(nullable = false)
	private int version;

	@Column(name = "previous_version_id")
	private UUID previousVersionId;

	@Column(name = "change_reason")
	private String changeReason;

	@Column(name = "created_by", nullable = false)
	private UUID createdBy;

	@Column(name = "updated_by")
	private UUID updatedBy;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "read_only", nullable = false)
	private boolean readOnly;

	@Column(nullable = false)
	private boolean deleted;

	@OneToMany(mappedBy = "diet", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@OrderBy("orderIndex")
	private List<MealJpaEntity> meals = new ArrayList<>();

	@OneToMany(mappedBy = "diet", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<SupplementJpaEntity> supplements = new ArrayList<>();

	protected DietJpaEntity() {
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public UUID getCoachId() {
		return coachId;
	}

	public void setCoachId(UUID coachId) {
		this.coachId = coachId;
	}

	public UUID getAthleteId() {
		return athleteId;
	}

	public void setAthleteId(UUID athleteId) {
		this.athleteId = athleteId;
	}

	public DietObjective getObjective() {
		return objective;
	}

	public void setObjective(DietObjective objective) {
		this.objective = objective;
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

	public DietStatus getStatus() {
		return status;
	}

	public void setStatus(DietStatus status) {
		this.status = status;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public UUID getPreviousVersionId() {
		return previousVersionId;
	}

	public void setPreviousVersionId(UUID previousVersionId) {
		this.previousVersionId = previousVersionId;
	}

	public String getChangeReason() {
		return changeReason;
	}

	public void setChangeReason(String changeReason) {
		this.changeReason = changeReason;
	}

	public UUID getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(UUID createdBy) {
		this.createdBy = createdBy;
	}

	public UUID getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(UUID updatedBy) {
		this.updatedBy = updatedBy;
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

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public List<MealJpaEntity> getMeals() {
		return meals;
	}

	public void setMeals(List<MealJpaEntity> meals) {
		this.meals = meals;
	}

	public void addMeal(MealJpaEntity meal) {
		meals.add(meal);
		meal.setDiet(this);
	}

	public void removeMeal(MealJpaEntity meal) {
		meals.remove(meal);
		meal.setDiet(null);
	}

	public List<SupplementJpaEntity> getSupplements() {
		return supplements;
	}

	public void setSupplements(List<SupplementJpaEntity> supplements) {
		this.supplements = supplements;
	}

	public void addSupplement(SupplementJpaEntity supplement) {
		supplements.add(supplement);
		supplement.setDiet(this);
	}
}
