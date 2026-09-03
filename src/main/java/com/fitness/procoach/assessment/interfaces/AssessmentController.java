package com.fitness.procoach.assessment.interfaces;

import com.fitness.procoach.assessment.application.AssessmentApplicationService;
import com.fitness.procoach.assessment.domain.Assessment;
import com.fitness.procoach.assessment.domain.AssessmentId;
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
 * Controller REST de avaliações físicas (US-004).
 */
@RestController
@RequestMapping("/api/v1/assessments")
public class AssessmentController {

	private final AssessmentApplicationService service;

	public AssessmentController(AssessmentApplicationService service) {
		this.service = service;
	}

	/**
	 * Registra uma nova avaliação. Retorna {@code 201 Created} com o DTO da
	 * avaliação no corpo e o header {@code Location} apontando para o recurso
	 * criado. Cada avaliação é um novo registro — avaliações anteriores nunca
	 * são sobrescritas.
	 */
	@PostMapping
	public ResponseEntity<AssessmentResponse> register(@Valid @RequestBody RegisterAssessmentRequest request) {
		Assessment assessment = service.registerAssessment(
				new AssessmentApplicationService.RegisterAssessmentCommand(
						request.athleteId(), request.coachId(), request.assessmentDate(),
						request.weightKg(), request.heightCm(), request.bodyFatPercentage(),
						request.muscleMassKg(), request.waistCm(), request.hipCm()));
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(assessment.id().value())
				.toUri();
		return ResponseEntity.created(location).body(AssessmentResponse.from(assessment));
	}

	/**
	 * Retorna uma avaliação pelo identificador.
	 */
	@GetMapping("/{id}")
	public AssessmentResponse findById(@PathVariable UUID id) {
		return AssessmentResponse.from(service.findById(new AssessmentId(id)));
	}

	/**
	 * Histórico de avaliações de um atleta em ordem cronológica (US-004).
	 */
	@GetMapping
	public List<AssessmentResponse> history(@RequestParam UUID athleteId) {
		return service.history(athleteId).stream().map(AssessmentResponse::from).toList();
	}
}
