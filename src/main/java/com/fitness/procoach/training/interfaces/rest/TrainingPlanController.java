package com.fitness.procoach.training.interfaces.rest;

import java.util.List;
import java.util.UUID;

import com.fitness.procoach.training.application.CreateTrainingPlanUseCase;
import com.fitness.procoach.training.application.CreateTrainingPlanVersionUseCase;
import com.fitness.procoach.training.application.SoftDeleteTrainingPlanUseCase;
import com.fitness.procoach.training.application.TrainingQueryService;
import com.fitness.procoach.training.application.TransitionTrainingPlanStatusUseCase;
import com.fitness.procoach.training.domain.model.TrainingPlan;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/training/plans")
public class TrainingPlanController {

	private final CreateTrainingPlanUseCase createTrainingPlanUseCase;

	private final CreateTrainingPlanVersionUseCase createVersionUseCase;

	private final TransitionTrainingPlanStatusUseCase transitionStatusUseCase;

	private final SoftDeleteTrainingPlanUseCase softDeleteUseCase;

	private final TrainingQueryService queryService;

	public TrainingPlanController(CreateTrainingPlanUseCase createTrainingPlanUseCase,
			CreateTrainingPlanVersionUseCase createVersionUseCase,
			TransitionTrainingPlanStatusUseCase transitionStatusUseCase,
			SoftDeleteTrainingPlanUseCase softDeleteUseCase, TrainingQueryService queryService) {
		this.createTrainingPlanUseCase = createTrainingPlanUseCase;
		this.createVersionUseCase = createVersionUseCase;
		this.transitionStatusUseCase = transitionStatusUseCase;
		this.softDeleteUseCase = softDeleteUseCase;
		this.queryService = queryService;
	}

	@PostMapping
	public ResponseEntity<TrainingPlanResponse> create(@RequestBody CreateTrainingPlanRequest request) {
		TrainingPlan plan = this.createTrainingPlanUseCase.execute(request.athleteId(), request.coachId(),
				request.periodizationId(), request.name(), request.description());
		return ResponseEntity.status(HttpStatus.CREATED).body(TrainingPlanResponse.from(plan));
	}

	@GetMapping("/{id}")
	public ResponseEntity<TrainingPlanResponse> findById(@PathVariable UUID id) {
		return this.queryService.findTrainingPlanById(id)
			.map(tp -> ResponseEntity.ok(TrainingPlanResponse.from(tp)))
			.orElse(ResponseEntity.notFound().build());
	}

	@GetMapping("/athlete/{athleteId}")
	public ResponseEntity<List<TrainingPlanResponse>> findByAthlete(@PathVariable UUID athleteId) {
		List<TrainingPlanResponse> result = this.queryService.findTrainingPlansByAthlete(athleteId)
			.stream()
			.map(TrainingPlanResponse::from)
			.toList();
		return ResponseEntity.ok(result);
	}

	@GetMapping("/athlete/{athleteId}/latest")
	public ResponseEntity<TrainingPlanResponse> findLatestVersion(@PathVariable UUID athleteId) {
		return this.queryService.findLatestPlanVersion(athleteId)
			.map(tp -> ResponseEntity.ok(TrainingPlanResponse.from(tp)))
			.orElse(ResponseEntity.notFound().build());
	}

	@PostMapping("/{id}/versions")
	public ResponseEntity<TrainingPlanResponse> createNewVersion(@PathVariable UUID id,
			@RequestBody CreateTrainingPlanVersionRequest request) {
		TrainingPlan newVersion = this.createVersionUseCase.execute(id, request.newName(), request.newDescription());
		return ResponseEntity.status(HttpStatus.CREATED).body(TrainingPlanResponse.from(newVersion));
	}

	@PostMapping("/{id}/transition")
	public ResponseEntity<TrainingPlanResponse> transitionStatus(@PathVariable UUID id,
			@RequestBody TransitionStatusRequest request) {
		TrainingPlan plan = this.transitionStatusUseCase.execute(id, request.targetStatus());
		return ResponseEntity.ok(TrainingPlanResponse.from(plan));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> softDelete(@PathVariable UUID id) {
		this.softDeleteUseCase.execute(id);
		return ResponseEntity.noContent().build();
	}

}
