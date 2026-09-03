package com.fitness.procoach.athlete.infrastructure;

import com.fitness.procoach.athlete.domain.AthleteStatus;
import com.fitness.procoach.athlete.domain.Goal;
import com.fitness.procoach.athlete.domain.Sex;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidade JPA do agregado Athlete. Mapeada para a tabela {@code athletes}
 * criada pela migração Flyway V200.
 */
@Entity
@Table(name = "athletes")
public class AthleteJpaEntity {

	@Id
	private UUID id;

	@Column(name = "coach_id", nullable = false)
	private UUID coachId;

	@Column(nullable = false, length = 150)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Sex sex;

	@Column(name = "birth_date", nullable = false)
	private LocalDate birthDate;

	@Column(nullable = false, precision = 5, scale = 2)
	private BigDecimal height;

	@Column(nullable = false, precision = 5, scale = 2)
	private BigDecimal weight;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private Goal goal;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AthleteStatus status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected AthleteJpaEntity() {
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Sex getSex() {
		return sex;
	}

	public void setSex(Sex sex) {
		this.sex = sex;
	}

	public LocalDate getBirthDate() {
		return birthDate;
	}

	public void setBirthDate(LocalDate birthDate) {
		this.birthDate = birthDate;
	}

	public BigDecimal getHeight() {
		return height;
	}

	public void setHeight(BigDecimal height) {
		this.height = height;
	}

	public BigDecimal getWeight() {
		return weight;
	}

	public void setWeight(BigDecimal weight) {
		this.weight = weight;
	}

	public Goal getGoal() {
		return goal;
	}

	public void setGoal(Goal goal) {
		this.goal = goal;
	}

	public AthleteStatus getStatus() {
		return status;
	}

	public void setStatus(AthleteStatus status) {
		this.status = status;
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
