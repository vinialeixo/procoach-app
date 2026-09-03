package com.fitness.procoach.assessment.infrastructure;

import com.fitness.procoach.assessment.domain.Assessment;
import com.fitness.procoach.assessment.domain.AssessmentId;
import com.fitness.procoach.assessment.domain.AssessmentRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adapter que implementa a porta {@link AssessmentRepository} sobre o
 * repositório Spring Data JPA.
 */
@Repository
public class AssessmentRepositoryAdapter implements AssessmentRepository {

	private final AssessmentJpaRepository jpaRepository;

	public AssessmentRepositoryAdapter(AssessmentJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	@Transactional
	public Assessment save(Assessment assessment) {
		return AssessmentMapper.toDomain(jpaRepository.save(AssessmentMapper.toEntity(assessment)));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Assessment> findById(AssessmentId id) {
		return jpaRepository.findById(id.value()).map(AssessmentMapper::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Assessment> findByAthleteIdOrderByAssessmentDateAsc(UUID athleteId) {
		return jpaRepository.findByAthleteIdOrderByAssessmentDateAsc(athleteId).stream()
				.map(AssessmentMapper::toDomain)
				.toList();
	}
}
