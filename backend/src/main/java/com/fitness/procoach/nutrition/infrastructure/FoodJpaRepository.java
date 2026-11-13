package com.fitness.procoach.nutrition.infrastructure;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositório Spring Data do catálogo de alimentos (T-086).
 */
public interface FoodJpaRepository extends JpaRepository<FoodJpaEntity, UUID> {

	@Query("""
			select f from FoodJpaEntity f
			where f.deleted = false
			  and (f.coachId is null or f.coachId = :coachId)
			  and (:category = '' or lower(f.category) = lower(:category))
			  and (:query = '' or lower(f.name) like concat('%', :query, '%'))
			""")
	Page<FoodJpaEntity> search(@Param("query") String query, @Param("category") String category,
			@Param("coachId") UUID coachId, Pageable pageable);
}
