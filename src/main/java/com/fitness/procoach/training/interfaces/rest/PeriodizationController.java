package com.fitness.procoach.training.interfaces.rest;

import java.util.List;
import java.util.UUID;

import com.fitness.procoach.training.application.CreatePeriodizationUseCase;
import com.fitness.procoach.training.application.TrainingQueryService;
import com.fitness.procoach.training.domain.model.Periodization;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/training/periodizations")
public class PeriodizationController {

	private final CreatePeriodizationUseCase createPeriodizationUseCase;

	private final TrainingQueryService queryService;

	public PeriodizationController(CreatePeriodizationUseCase createPeriodizationUseCase,
			TrainingQueryService queryService) {
		this.createPeriodizationUseCase = createPeriodizationUseCase;
		this.queryService = queryService;
	}

	@PostMapping
	public ResponseEntity<PeriodizationResponse> create(@RequestBody CreatePeriodizationRequest request) {
		Periodization periodization = this.createPeriodizationUseCase.execute(request.athleteId(), request.coachId(),
				request.name(), request.description(), request.startDate(), request.endDate());
		return ResponseEntity.status(HttpStatus.CREATED).body(PeriodizationResponse.from(periodization));
	}

	@GetMapping("/{id}")
	public ResponseEntity<PeriodizationResponse> findById(@PathVariable UUID id) {
		return this.queryService.findPeriodizationById(id)
			.map(p -> ResponseEntity.ok(PeriodizationResponse.from(p)))
			.orElse(ResponseEntity.notFound().build());
	}

	@GetMapping("/athlete/{athleteId}")
	public ResponseEntity<List<PeriodizationResponse>> findByAthlete(@PathVariable UUID athleteId) {
		List<PeriodizationResponse> result = this.queryService.findPeriodizationsByAthlete(athleteId)
			.stream()
			.map(PeriodizationResponse::from)
			.toList();
		return ResponseEntity.ok(result);
	}

}
