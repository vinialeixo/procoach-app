package com.fitness.procoach.nutrition.interfaces;

import com.fitness.procoach.nutrition.application.FoodApplicationService;
import com.fitness.procoach.nutrition.domain.Food;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Controller REST do catálogo de alimentos (T-086). Consulta paginada de
 * alimentos TACO/globais e criação de alimentos CUSTOM pelo coach autenticado.
 * O coachId nunca vem do request: é resolvido a partir do JWT.
 */
@Tag(name = "Foods", description = "Catálogo TACO de alimentos com macros por 100 g, porção "
		+ "comestível, medida caseira e estado padrão (RAW/AS_SERVED). Alimentos CUSTOM são "
		+ "vinculados ao coach autenticado e não aparecem para outros coaches.")
@RestController
@RequestMapping("/api/v1/foods")
@PreAuthorize("hasRole('COACH')")
public class FoodController {

	private final FoodApplicationService service;

	public FoodController(FoodApplicationService service) {
		this.service = service;
	}

	@Operation(summary = "Buscar alimentos (paginado)",
			description = "Busca por trecho do nome e categoria opcional. Retorna alimentos globais "
					+ "(TACO/USDA) e os CUSTOM do coach autenticado, nunca os de outro coach.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Página de alimentos retornada"),
			@ApiResponse(responseCode = "400", description = "Parâmetros de paginação inválidos")})
	@GetMapping
	public FoodPageResponse search(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Trecho do nome do alimento (case-insensitive)")
			@RequestParam(required = false) String query,
			@Parameter(description = "Categoria exata do alimento")
			@RequestParam(required = false) String category,
			@Parameter(description = "Número da página (base 0)")
			@RequestParam(defaultValue = "0") int page,
			@Parameter(description = "Tamanho da página (1..100)")
			@RequestParam(defaultValue = "20") int size) {
		return FoodPageResponse.from(service.search(UUID.fromString(jwt.getSubject()), query, category, page, size));
	}

	@Operation(summary = "Buscar alimento por id",
			description = "Retorna um alimento do catálogo global ou CUSTOM do próprio coach. Alimento "
					+ "de outro coach retorna 404 (não vaza a existência do recurso).")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Alimento encontrado"),
			@ApiResponse(responseCode = "404", description = "Alimento não encontrado ou de outro coach")})
	@GetMapping("/{id}")
	public FoodResponse findById(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Identificador do alimento") @PathVariable UUID id) {
		return FoodResponse.from(service.findById(UUID.fromString(jwt.getSubject()), id));
	}

	@Operation(summary = "Criar alimento CUSTOM",
			description = "Cria um alimento CUSTOM vinculado ao coach autenticado (coachApproved=true). "
					+ "Toda escrita gera registro de auditoria.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Alimento CUSTOM criado"),
			@ApiResponse(responseCode = "400", description = "Campos inválidos ou ausentes")})
	@PostMapping
	public ResponseEntity<FoodResponse> createCustom(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CreateFoodRequest request) {
		Food food = service.createCustom(new FoodApplicationService.CreateCustomFoodCommand(
				UUID.fromString(jwt.getSubject()),
				request.name(),
				request.category(),
				request.calories(),
				request.protein(),
				request.carbohydrates(),
				request.fat(),
				request.ediblePortionPercent(),
				request.householdMeasure(),
				request.householdMeasureGrams(),
				request.stateDefault()));
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(food.id().value())
				.toUri();
		return ResponseEntity.created(location).body(FoodResponse.from(food));
	}
}
