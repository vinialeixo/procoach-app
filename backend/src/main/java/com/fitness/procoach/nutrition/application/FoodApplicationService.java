package com.fitness.procoach.nutrition.application;

import com.fitness.procoach.audit.application.AuditData;
import com.fitness.procoach.audit.application.AuditService;
import com.fitness.procoach.coach.application.CoachApplicationService;
import com.fitness.procoach.coach.domain.CoachId;
import com.fitness.procoach.nutrition.domain.Food;
import com.fitness.procoach.nutrition.domain.FoodId;
import com.fitness.procoach.nutrition.domain.FoodMacros;
import com.fitness.procoach.nutrition.domain.FoodPage;
import com.fitness.procoach.nutrition.domain.FoodRepository;
import com.fitness.procoach.nutrition.domain.FoodState;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service do catálogo de alimentos TACO (T-086).
 *
 * <p>O {@code coachId} nunca vem do request: é sempre resolvido server-side a
 * partir do usuário autenticado (subject do JWT), via fachada do módulo coach
 * (T-058).</p>
 *
 * <p>Framework-agnostic: registrado como bean pela configuração da camada de
 * infraestrutura.</p>
 */
public class FoodApplicationService {

	private static final int MAX_PAGE_SIZE = 100;

	private final FoodRepository repository;
	private final CoachApplicationService coachService;
	private final AuditService auditService;

	public FoodApplicationService(FoodRepository repository, CoachApplicationService coachService,
			AuditService auditService) {
		this.repository = repository;
		this.coachService = coachService;
		this.auditService = auditService;
	}

	/**
	 * Busca paginada no catálogo: alimentos globais + CUSTOM do coach autenticado.
	 * Filtros opcionais por nome e categoria.
	 */
	public FoodPage search(UUID userId, String query, String category, int page, int size) {
		if (page < 0) {
			throw new IllegalArgumentException("page must be >= 0");
		}
		if (size < 1 || size > MAX_PAGE_SIZE) {
			throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
		}
		CoachId coachId = coachService.requireCoachIdByUserId(userId);
		return repository.search(query, category, coachId.value(), page, size);
	}

	/**
	 * Busca por id. Alimento CUSTOM de outro coach (ou excluído) resulta em 404 —
	 * o id do recurso nunca é suficiente para acessá-lo.
	 */
	public Food findById(UUID userId, UUID foodId) {
		CoachId coachId = coachService.requireCoachIdByUserId(userId);
		Food food = repository.findById(new FoodId(foodId))
				.filter(found -> !found.isDeleted())
				.orElseThrow(() -> new FoodNotFoundException(foodId));
		if (food.isCustom() && !coachId.value().equals(food.coachId())) {
			throw new FoodNotFoundException(foodId);
		}
		return food;
	}

	/**
	 * Cria um alimento CUSTOM vinculado ao coach autenticado. Toda escrita gera
	 * registro de auditoria (append-only).
	 */
	@Transactional
	public Food createCustom(CreateCustomFoodCommand command) {
		CoachId coachId = coachService.requireCoachIdByUserId(command.userId());
		double ediblePortion = command.ediblePortionPercent() != null
				? command.ediblePortionPercent() : 100.0;
		Food food = Food.createCustom(
				coachId.value(),
				command.name(),
				command.category(),
				FoodMacros.of(command.calories(), command.protein(), command.carbohydrates(), command.fat()),
				ediblePortion,
				command.householdMeasure(),
				command.householdMeasureGrams(),
				command.stateDefault());
		Food saved = repository.save(food);
		auditService.record("Food", saved.id().value(), "FOOD_CUSTOM_CREATED", command.userId(),
				AuditData.of("name", saved.name(), "category", saved.category(),
						"stateDefault", saved.stateDefault().name()));
		return saved;
	}

	public record CreateCustomFoodCommand(UUID userId, String name, String category, double calories,
			double protein, double carbohydrates, double fat, Double ediblePortionPercent,
			String householdMeasure, Double householdMeasureGrams, FoodState stateDefault) {
	}
}
