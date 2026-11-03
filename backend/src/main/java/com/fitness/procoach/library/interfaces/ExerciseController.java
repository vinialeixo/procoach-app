package com.fitness.procoach.library.interfaces;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Controller REST da Biblioteca de Exercícios. */
@Tag(name = "Library", description = "Biblioteca de exercícios com execução, erros comuns, progressões e referências científicas.")
@RestController
@RequestMapping("/api/library/exercises")
public class ExerciseController {

	private final ExerciseApplicationService service;

	public ExerciseController(ExerciseApplicationService service) {
		this.service = service;
	}

	@Operation(summary = "Criar exercício",
			description = "Cadastra um novo exercício na biblioteca com execução, erros comuns, progressões, "
					+ "regressões, contraindicações e referências científicas (US-019). Retorna 201 Created.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Exercício criado com sucesso"),
			@ApiResponse(responseCode = "400", description = "Campos inválidos ou ausentes") })
	// T-009/US-037: somente o coach gerencia o conteúdo da biblioteca.
	@PreAuthorize("hasRole('COACH')")
	@PostMapping
	public ResponseEntity<ExerciseResponse> create(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CreateExerciseRequest request) {
		// createdBy (autoria/ownership) é sempre o subject do JWT — nunca vem do corpo.
		Exercise created = service.create(new ExerciseApplicationService.CreateExerciseCommand(
				request.name(), request.muscleGroups(), request.equipment(), request.execution(),
				request.commonErrors(), request.progressions(), request.regressions(),
				request.contraindications(), request.references(), UUID.fromString(jwt.getSubject())));
		return ResponseEntity.status(HttpStatus.CREATED).body(ExerciseResponse.from(created));
	}

	@Operation(summary = "Atualizar exercício",
			description = "Atualiza os dados descritivos de um exercício da biblioteca (id, createdBy e createdAt "
					+ "são imutáveis).")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Exercício atualizado com sucesso"),
			@ApiResponse(responseCode = "400", description = "Campos inválidos ou ausentes"),
			@ApiResponse(responseCode = "404", description = "Exercício não encontrado ou de outro coach") })
	// T-009/US-037: somente o coach gerencia o conteúdo da biblioteca.
	@PreAuthorize("hasRole('COACH')")
	@PutMapping("/{id}")
	public ResponseEntity<ExerciseResponse> update(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Identificador do exercício") @PathVariable UUID id,
			@Valid @RequestBody UpdateExerciseRequest request) {
		Exercise updated = service.update(id, new ExerciseApplicationService.UpdateExerciseCommand(
				request.name(), request.muscleGroups(), request.equipment(), request.execution(),
				request.commonErrors(), request.progressions(), request.regressions(),
				request.contraindications(), request.references()), UUID.fromString(jwt.getSubject()));
		return ResponseEntity.ok(ExerciseResponse.from(updated));
	}

	@Operation(summary = "Remover exercício (soft-delete)",
			description = "Exclusão lógica do exercício: o registro é apenas marcado como excluído e nunca "
					+ "removido fisicamente do banco (regra inegociável de soft-delete).")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Exercício removido (soft-delete)"),
			@ApiResponse(responseCode = "404", description = "Exercício não encontrado") })
	// T-009/US-037: somente o coach gerencia o conteúdo da biblioteca.
	@PreAuthorize("hasRole('COACH')")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Identificador do exercício") @PathVariable UUID id) {
		service.delete(id, UUID.fromString(jwt.getSubject()));
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Buscar exercício por id")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Exercício encontrado"),
			@ApiResponse(responseCode = "404", description = "Exercício não encontrado") })
	@GetMapping("/{id}")
	public ResponseEntity<ExerciseResponse> findById(
			@Parameter(description = "Identificador do exercício") @PathVariable UUID id) {
		return ResponseEntity.ok(ExerciseResponse.from(service.findById(id)));
	}

	@Operation(summary = "Buscar exercícios",
			description = "Lista exercícios da biblioteca com filtros opcionais por nome (contém, "
					+ "case-insensitive) e/ou grupo muscular.")
	@ApiResponse(responseCode = "200", description = "Busca realizada com sucesso")
	@GetMapping
	public ResponseEntity<List<ExerciseResponse>> search(
			@Parameter(description = "Trecho do nome do exercício", example = "supino")
			@RequestParam(required = false) String name,
			@Parameter(description = "Grupo muscular trabalhado", example = "PEITO")
			@RequestParam(required = false) MuscleGroup muscleGroup) {
		List<ExerciseResponse> result = service.search(name, muscleGroup).stream()
				.map(ExerciseResponse::from)
				.toList();
		return ResponseEntity.ok(result);
	}
}
