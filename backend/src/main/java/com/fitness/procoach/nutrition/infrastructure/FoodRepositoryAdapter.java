package com.fitness.procoach.nutrition.infrastructure;

import com.fitness.procoach.nutrition.domain.Food;
import com.fitness.procoach.nutrition.domain.FoodId;
import com.fitness.procoach.nutrition.domain.FoodPage;
import com.fitness.procoach.nutrition.domain.FoodRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adapter que implementa a porta {@link FoodRepository} sobre o repositório
 * Spring Data JPA.
 */
@Repository
public class FoodRepositoryAdapter implements FoodRepository {

	private final FoodJpaRepository jpaRepository;

	public FoodRepositoryAdapter(FoodJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	@Transactional
	public Food save(Food food) {
		return FoodMapper.toDomain(jpaRepository.save(FoodMapper.toEntity(food)));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Food> findById(FoodId id) {
		return jpaRepository.findById(id.value()).map(FoodMapper::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public FoodPage search(String query, String category, UUID coachId, int page, int size) {
		String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
		String normalizedCategory = category == null || category.isBlank() ? "" : category.trim();
		PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Order.asc("name")));
		Page<FoodJpaEntity> result = jpaRepository.search(normalizedQuery, normalizedCategory, coachId, pageable);
		return new FoodPage(result.getContent().stream().map(FoodMapper::toDomain).toList(),
				result.getNumber(), result.getSize(), result.getTotalElements());
	}
}
