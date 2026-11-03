package com.fitness.procoach.assessment.infrastructure;

import com.fitness.procoach.assessment.domain.Assessment;
import com.fitness.procoach.assessment.domain.AssessmentId;
import com.fitness.procoach.assessment.domain.BodyMeasurements;

/**
 * Mapeamento entre o agregado de domínio e a entidade JPA.
 */
final class AssessmentMapper {

	private AssessmentMapper() {
	}

	static AssessmentJpaEntity toEntity(Assessment assessment) {
		AssessmentJpaEntity entity = new AssessmentJpaEntity();
		entity.setId(assessment.id().value());
		entity.setAthleteId(assessment.athleteId());
		entity.setCoachId(assessment.coachId());
		entity.setAssessmentDate(assessment.assessmentDate());
		BodyMeasurements m = assessment.measurements();
		entity.setWeightKg(m.weightKg());
		entity.setHeightCm(m.heightCm());
		entity.setBodyFatPercentage(m.bodyFatPercentage());
		entity.setMuscleMassKg(m.muscleMassKg());
		entity.setWaistCm(m.waistCm());
		entity.setHipCm(m.hipCm());
		entity.setCreatedAt(assessment.createdAt());
		entity.setUpdatedAt(assessment.updatedAt());
		entity.setVersion(assessment.version());
		return entity;
	}

	static Assessment toDomain(AssessmentJpaEntity entity) {
		return Assessment.reconstitute(
				new AssessmentId(entity.getId()),
				entity.getAthleteId(),
				entity.getCoachId(),
				entity.getAssessmentDate(),
				new BodyMeasurements(entity.getWeightKg(), entity.getHeightCm(),
						entity.getBodyFatPercentage(), entity.getMuscleMassKg(),
						entity.getWaistCm(), entity.getHipCm()),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getVersion());
	}
}
