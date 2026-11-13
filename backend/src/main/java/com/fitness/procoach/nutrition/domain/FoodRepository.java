package com.fitness.procoach.nutrition.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * Porta de persistência do catálogo de alimentos (T-086). Implementada na
 * camada de infraestrutura.
 */
public interface FoodRepository {

    Food save(Food food);

    Optional<Food> findById(FoodId id);

    /**
     * Busca paginada por nome (trecho, case-insensitive) e categoria opcional.
     *
     * <p>Retorna apenas alimentos globais (TACO/USDA) e os alimentos
     * {@code CUSTOM} do coach informado — nunca os de outro coach — e nunca
     * registros excluídos logicamente.</p>
     */
    FoodPage search(String query, String category, UUID coachId, int page, int size);
}
