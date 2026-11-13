package com.fitness.procoach.nutrition.domain;

import java.util.List;
import java.util.Objects;

/**
 * Página de resultados de busca no catálogo de alimentos (T-086).
 * Framework-agnostic: não depende de {@code org.springframework.data.domain.Page}.
 */
public record FoodPage(List<Food> content, int page, int size, long totalElements) {

    public FoodPage {
        content = List.copyOf(Objects.requireNonNull(content, "content"));
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size must be > 0");
        }
        if (totalElements < 0) {
            throw new IllegalArgumentException("totalElements must be >= 0");
        }
    }

    public int totalPages() {
        return size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    }
}
