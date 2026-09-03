package com.fitness.procoach.nutrition.interfaces;

import com.fitness.procoach.nutrition.application.DietApplicationService;
import com.fitness.procoach.nutrition.domain.Diet;
import com.fitness.procoach.nutrition.domain.DietId;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Controller REST de dietas (US-007). Cria a primeira versão (v1) e consulta a
 * versão mais recente ou o histórico de versões de um atleta.
 */
@RestController
@RequestMapping("/api/v1/diets")
public class DietController {

	private final DietApplicationService service;

	public DietController(DietApplicationService service) {
		this.service = service;
	}

	/**
	 * Cria a primeira versão (v1) de uma dieta. Retorna {@code 201 Created} com
	 * o DTO no corpo e o header {@code Location} apontando para o recurso.
	 */
	@PostMapping
	public ResponseEntity<DietResponse> create(@Valid @RequestBody CreateDietRequest request) {
		Diet diet = service.createDiet(
				new DietApplicationService.CreateDietCommand(
						request.coachId(), request.athleteId(), request.objective(),
						request.calories(), request.protein(), request.carbohydrates(),
						request.fat(), request.createdBy()));
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(diet.id().value())
				.toUri();
		return ResponseEntity.created(location).body(DietResponse.from(diet));
	}

	/**
	 * Retorna uma dieta pelo identificador.
	 */
	@GetMapping("/{id}")
	public DietResponse findById(@PathVariable UUID id) {
		return DietResponse.from(service.findById(new DietId(id)));
	}

	/**
	 * Versão mais recente da dieta de um atleta (US-007).
	 */
	@GetMapping
	public DietResponse latest(@RequestParam UUID athleteId) {
		return DietResponse.from(service.latestByAthlete(athleteId));
	}

	/**
	 * Histórico de versões da dieta de um atleta.
	 */
	@GetMapping("/history")
	public List<DietResponse> history(@RequestParam UUID athleteId) {
		return service.historyByAthlete(athleteId).stream().map(DietResponse::from).toList();
	}
}
