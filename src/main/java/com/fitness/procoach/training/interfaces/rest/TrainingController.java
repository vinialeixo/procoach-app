package com.fitness.procoach.training.interfaces.rest;

import java.util.List;
import java.util.UUID;

import com.fitness.procoach.training.application.AddExerciseToTrainingUseCase;
import com.fitness.procoach.training.application.CreateTrainingUseCase;
import com.fitness.procoach.training.application.ReorderExercisesUseCase;
import com.fitness.procoach.training.application.SoftDeleteTrainingUseCase;
import com.fitness.procoach.training.application.TrainingQueryService;
import com.fitness.procoach.training.application.TransitionTrainingStatusUseCase;
import com.fitness.procoach.training.domain.model.Training;
import com.fitness.procoach.training.domain.model.TrainingStatus;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/training/trainings")
public class TrainingController {

	private final CreateTrainingUseCase createTrainingUseCase;

	private final AddExerciseToTrainingUseCase addExerciseUseCase;

	private final ReorderExercisesUseCase reorderExercisesUseCase;

	private final TransitionTrainingStatusUseCase transitionStatusUseCase;

	private final SoftDeleteTrainingUseCase softDeleteUseCase;

	private final TrainingQueryService queryService;

	public TrainingController(CreateTrainingUseCase createTrainingUseCase,
			AddExerciseToTrainingUseCase addExerciseUseCase, ReorderExercisesUseCase reorderExercisesUseCase,
			TransitionTrainingStatusUseCase transitionStatusUseCase, SoftDeleteTrainingUseCase softDeleteUseCase,
			TrainingQueryService queryService) {
		this.createTrainingUseCase = createTrainingUseCase;
		this.addExerciseUseCase = addExerciseUseCase;
		this.reorderExercisesUseCase = reorderExercisesUseCase;
		this.transitionStatusUseCase = transitionStatusUseCase;
		this.softDeleteUseCase = softDeleteUseCase;
		this.queryService = queryService;
	}

	@PostMapping
	public ResponseEntity<TrainingResponse> create(@RequestBody CreateTrainingRequest request) {
		Training training = this.createTrainingUseCase.execute(request.periodizationId(), request.athleteId(),
				request.coachId(), request.name(), request.description(), request.weekNumber(), request.dayOfWeek());
		return ResponseEntity.status(HttpStatus.CREATED).body(TrainingResponse.from(training));
	}

	@GetMapping("/{id}")
	public ResponseEntity<TrainingResponse> findById(@PathVariable UUID id) {
		return this.queryService.findTrainingById(id)
			.map(t -> ResponseEntity.ok(TrainingResponse.from(t)))
			.orElse(ResponseEntity.notFound().build());
	}

	@GetMapping("/periodization/{periodizationId}")
	public ResponseEntity<List<TrainingResponse>> findByPeriodization(@PathVariable UUID periodizationId) {
		List<TrainingResponse> result = this.queryService.findTrainingsByPeriodization(periodizationId)
			.stream()
			.map(TrainingResponse::from)
			.toList();
		return ResponseEntity.ok(result);
	}

	@GetMapping("/athlete/{athleteId}")
	public ResponseEntity<List<TrainingResponse>> findByAthlete(@PathVariable UUID athleteId,
			@RequestParam(required = false) TrainingStatus status) {
		List<Training> trainings = (status != null)
				? this.queryService.findTrainingsByAthleteAndStatus(athleteId, status)
				: this.queryService.findTrainingsByAthlete(athleteId);
		return ResponseEntity.ok(trainings.stream().map(TrainingResponse::from).toList());
	}

	@PostMapping("/{id}/exercises")
	public ResponseEntity<TrainingResponse> addExercise(@PathVariable UUID id,
			@RequestBody AddExerciseRequest request) {
		Training training = this.addExerciseUseCase.execute(id, request.exerciseId(), request.exerciseName(),
				request.customExercise(), request.orderIndex(), request.sets(), request.repetitions(), request.load(),
				request.intensityType(), request.intensityValue(), request.restSeconds(), request.notes());
		return ResponseEntity.ok(TrainingResponse.from(training));
	}

	@PutMapping("/{id}/exercises/reorder")
	public ResponseEntity<TrainingResponse> reorderExercises(@PathVariable UUID id,
			@RequestBody ReorderExercisesRequest request) {
		Training training = this.reorderExercisesUseCase.execute(id, request.orderedPrescriptionIds());
		return ResponseEntity.ok(TrainingResponse.from(training));
	}

	@PostMapping("/{id}/transition")
	public ResponseEntity<TrainingResponse> transitionStatus(@PathVariable UUID id,
			@RequestBody TransitionStatusRequest request) {
		Training training = this.transitionStatusUseCase.execute(id, request.targetStatus());
		return ResponseEntity.ok(TrainingResponse.from(training));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> softDelete(@PathVariable UUID id) {
		this.softDeleteUseCase.execute(id);
		return ResponseEntity.noContent().build();
	}

}
