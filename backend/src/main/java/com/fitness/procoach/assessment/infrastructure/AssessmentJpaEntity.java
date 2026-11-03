package com.fitness.procoach.assessment.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidade JPA do agregado Assessment. Mapeada para a tabela {@code assessments}
 * criada pela migração Flyway V300.
 */
@Entity
@Table(name = "assessments")
public class AssessmentJpaEntity {

	@Id
	private UUID id;

	@Column(name = "athlete_id", nullable = false)
	private UUID athleteId;

	@Column(name = "coach_id", nullable = false)
	private UUID coachId;

	@Column(name = "assessment_date", nullable = false)
	private LocalDate assessmentDate;

	@Column(name = "weight_kg", nullable = false, precision = 5, scale = 2)
	private BigDecimal weightKg;

	@Column(name = "height_cm", nullable = false, precision = 5, scale = 2)
	private BigDecimal heightCm;

	@Column(name = "body_fat_percentage", precision = 5, scale = 2)
	private BigDecimal bodyFatPercentage;

	@Column(name = "muscle_mass_kg", precision = 5, scale = 2)
	private BigDecimal muscleMassKg;

	@Column(name = "waist_cm", precision = 5, scale = 2)
	private BigDecimal waistCm;

	@Column(name = "hip_cm", precision = 5, scale = 2)
	private BigDecimal hipCm;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(nullable = false)
	private long version;

	protected AssessmentJpaEntity() {
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public UUID getAthleteId() {
		return athleteId;
	}

	public void setAthleteId(UUID athleteId) {
		this.athleteId = athleteId;
	}

	public UUID getCoachId() {
		return coachId;
	}

	public void setCoachId(UUID coachId) {
		this.coachId = coachId;
	}

	public LocalDate getAssessmentDate() {
		return assessmentDate;
	}

	public void setAssessmentDate(LocalDate assessmentDate) {
		this.assessmentDate = assessmentDate;
	}

	public BigDecimal getWeightKg() {
		return weightKg;
	}

	public void setWeightKg(BigDecimal weightKg) {
		this.weightKg = weightKg;
	}

	public BigDecimal getHeightCm() {
		return heightCm;
	}

	public void setHeightCm(BigDecimal heightCm) {
		this.heightCm = heightCm;
	}

	public BigDecimal getBodyFatPercentage() {
		return bodyFatPercentage;
	}

	public void setBodyFatPercentage(BigDecimal bodyFatPercentage) {
		this.bodyFatPercentage = bodyFatPercentage;
	}

	public BigDecimal getMuscleMassKg() {
		return muscleMassKg;
	}

	public void setMuscleMassKg(BigDecimal muscleMassKg) {
		this.muscleMassKg = muscleMassKg;
	}

	public BigDecimal getWaistCm() {
		return waistCm;
	}

	public void setWaistCm(BigDecimal waistCm) {
		this.waistCm = waistCm;
	}

	public BigDecimal getHipCm() {
		return hipCm;
	}

	public void setHipCm(BigDecimal hipCm) {
		this.hipCm = hipCm;
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

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}
}
