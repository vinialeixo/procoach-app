package com.fitness.procoach.nutrition.interfaces;

import com.fitness.procoach.nutrition.domain.FoodPage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * DTO de saída da busca paginada do catálogo de alimentos (T-086).
 */
@Schema(description = "Página de alimentos do catálogo (globais + CUSTOM do coach autenticado).")
public record FoodPageResponse(
		@Schema(description = "Alimentos da página") List<FoodResponse> content,
		@Schema(description = "Número da página (base 0)", example = "0") int page,
		@Schema(description = "Tamanho da página", example = "20") int size,
		@Schema(description = "Total de alimentos encontrados", example = "5") long totalElements,
		@Schema(description = "Total de páginas", example = "1") int totalPages) {

	public static FoodPageResponse from(FoodPage result) {
		return new FoodPageResponse(
				result.content().stream().map(FoodResponse::from).toList(),
				result.page(),
				result.size(),
				result.totalElements(),
				result.totalPages());
	}
}
