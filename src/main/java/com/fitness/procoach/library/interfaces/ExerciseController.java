package com.fitness.procoach.library.interfaces;

import java.util.List;
import java.util.UUID;

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

import com.fitness.procoach.library.application.ExerciseApplicationService;
import com.fitness.procoach.library.domain.Exercise;
import com.fitness.procoach.library.domain.MuscleGroup;

import jakarta.validation.Valid;

/** Controller REST da Biblioteca de Exercícios. */
@RestController
@RequestMapping("/api/library/exercises")
public class ExerciseController {

	private final ExerciseApplicationService service;

	public ExerciseController(ExerciseApplicationService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<ExerciseResponse> create(@Valid @RequestBody CreateExerciseRequest request) {
		Exercise created = service.create(new ExerciseApplicationService.CreateExerciseCommand(
				request.name(), request.muscleGroups(), request.equipment(), request.execution(),
				request.commonErrors(), request.progressions(), request.regressions(),
				request.contraindications(), request.references(), request.createdBy()));
		return ResponseEntity.status(HttpStatus.CREATED).body(ExerciseResponse.from(created));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ExerciseResponse> update(@PathVariable UUID id,
			@Valid @RequestBody UpdateExerciseRequest request) {
		Exercise updated = service.update(id, new ExerciseApplicationService.UpdateExerciseCommand(
				request.name(), request.muscleGroups(), request.equipment(), request.execution(),
				request.commonErrors(), request.progressions(), request.regressions(),
				request.contraindications(), request.references()));
		return ResponseEntity.ok(ExerciseResponse.from(updated));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{id}")
	public ResponseEntity<ExerciseResponse> findById(@PathVariable UUID id) {
		return ResponseEntity.ok(ExerciseResponse.from(service.findById(id)));
	}

	@GetMapping
	public ResponseEntity<List<ExerciseResponse>> search(
			@RequestParam(required = false) String name,
			@RequestParam(required = false) MuscleGroup muscleGroup) {
		List<ExerciseResponse> result = service.search(name, muscleGroup).stream()
				.map(ExerciseResponse::from)
				.toList();
		return ResponseEntity.ok(result);
	}
}
