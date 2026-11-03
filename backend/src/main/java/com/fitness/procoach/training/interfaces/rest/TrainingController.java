package com.fitness.procoach.training.interfaces.rest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fitness.procoach.athlete.application.AthleteAccessService;
import com.fitness.procoach.coach.application.CoachApplicationService;
import com.fitness.procoach.training.application.AddExerciseToTrainingUseCase;
import com.fitness.procoach.training.application.CreateTrainingUseCase;
import com.fitness.procoach.training.application.ReorderExercisesUseCase;
import com.fitness.procoach.training.application.SoftDeleteTrainingUseCase;
import com.fitness.procoach.training.application.TrainingQueryService;
import com.fitness.procoach.training.application.TransitionTrainingStatusUseCase;
import com.fitness.procoach.training.domain.model.Periodization;
import com.fitness.procoach.training.domain.model.Training;
import com.fitness.procoach.training.domain.model.TrainingStatus;

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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Training",
		description = "Periodizações, planos de treino e treinos (US-010/011/012). Planos seguem a mesma regra "
				+ "inegociável de nunca sobrescrever: alterações geram uma nova versão encadeada e exclusões são "
				+ "sempre soft-delete.")
@RestController
@RequestMapping("/api/training/trainings")
public class TrainingController {

	private final CreateTrainingUseCase createTrainingUseCase;

	private final AddExerciseToTrainingUseCase addExerciseUseCase;

	private final ReorderExercisesUseCase reorderExercisesUseCase;

	private final TransitionTrainingStatusUseCase transitionStatusUseCase;

	private final SoftDeleteTrainingUseCase softDeleteUseCase;

	private final TrainingQueryService queryService;

	private final CoachApplicationService coachService;

	private final AthleteAccessService athleteAccessService;

	public TrainingController(CreateTrainingUseCase createTrainingUseCase,
			AddExerciseToTrainingUseCase addExerciseUseCase, ReorderExercisesUseCase reorderExercisesUseCase,
			TransitionTrainingStatusUseCase transitionStatusUseCase, SoftDeleteTrainingUseCase softDeleteUseCase,
			TrainingQueryService queryService, CoachApplicationService coachService,
			AthleteAccessService athleteAccessService) {
		this.createTrainingUseCase = createTrainingUseCase;
		this.addExerciseUseCase = addExerciseUseCase;
		this.reorderExercisesUseCase = reorderExercisesUseCase;
		this.transitionStatusUseCase = transitionStatusUseCase;
		this.softDeleteUseCase = softDeleteUseCase;
		this.queryService = queryService;
		this.coachService = coachService;
		this.athleteAccessService = athleteAccessService;
	}

	@Operation(summary = "Criar treino",
			description = "Cria um treino (sessão) vinculado a uma periodização, com semana e dia da semana. "
					+ "Retorna 201 Created.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Treino criado com sucesso"),
			@ApiResponse(responseCode = "400", description = "Dados inválidos ou periodização inexistente") })
	// T-009/US-037: somente o coach gerencia protocolos.
	@PreAuthorize("hasRole('COACH')")
	@PostMapping
	public ResponseEntity<TrainingResponse> create(@AuthenticationPrincipal Jwt jwt,
			@RequestBody CreateTrainingRequest request) {
		// T-058: o coachId nunca vem do request — é resolvido server-side a partir
		// do usuário autenticado (subject do JWT) na camada de aplicação.
		Training training = this.createTrainingUseCase.execute(request.periodizationId(), request.athleteId(),
				UUID.fromString(jwt.getSubject()), request.name(), request.description(), request.weekNumber(),
				request.dayOfWeek());
		return ResponseEntity.status(HttpStatus.CREATED).body(TrainingResponse.from(training));
	}

	@Operation(summary = "Buscar treino por id")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Treino encontrado"),
			@ApiResponse(responseCode = "404", description = "Treino não encontrado") })
	// T-009/T-010: acesso direto por id é restrito ao coach — o atleta usa
	// GET /athlete/{athleteId} (ancorado no claim do JWT).
	// T-058 (anti-IDOR): o coach só acessa treino de atleta da própria carteira —
	// o id de um recurso de outro coach resulta em 404 (a guarda da camada de
	// aplicação lança AthleteNotFoundException).
	@PreAuthorize("hasRole('COACH')")
	@GetMapping("/{id}")
	public ResponseEntity<TrainingResponse> findById(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Identificador do treino") @PathVariable UUID id) {
		return this.queryService.findTrainingById(id, UUID.fromString(jwt.getSubject()))
			.map(t -> ResponseEntity.ok(TrainingResponse.from(t)))
			.orElse(ResponseEntity.notFound().build());
	}

	@Operation(summary = "Listar treinos da periodização",
			description = "Lista os treinos (sessões) de uma periodização — visão de montagem do coach.")
	@ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
	// T-009: listagem por periodização é uma visão de montagem do coach.
	// T-058 (anti-IDOR): a periodização precisa existir E pertencer a um atleta
	// da carteira do coach autenticado — caso contrário, 404.
	@PreAuthorize("hasRole('COACH')")
	@GetMapping("/periodization/{periodizationId}")
	public ResponseEntity<List<TrainingResponse>> findByPeriodization(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Identificador da periodização") @PathVariable UUID periodizationId) {
		Optional<Periodization> periodization = this.queryService
				.findPeriodizationById(periodizationId, UUID.fromString(jwt.getSubject()));
		if (periodization.isEmpty()) {
			return ResponseEntity.notFound().build();
		}
		List<TrainingResponse> result = this.queryService.findTrainingsByPeriodization(periodizationId)
			.stream()
			.map(TrainingResponse::from)
			.toList();
		return ResponseEntity.ok(result);
	}

	@Operation(summary = "Listar treinos do atleta",
			description = "Lista os treinos de um atleta, com filtro opcional por status. O atleta só consulta "
					+ "os próprios dados (claim athleteId do JWT — US-038).")
	@ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
	// T-010/US-038: atleta só consulta os próprios dados (claim athleteId do JWT).
	// T-058: coach só consulta atletas da própria carteira — o athleteId de outro
	// coach resulta em 404 (não vaza a existência do recurso).
	@PreAuthorize("@athleteGuard.canAccess(authentication, #athleteId)")
	@GetMapping("/athlete/{athleteId}")
	public ResponseEntity<List<TrainingResponse>> findByAthlete(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Identificador do atleta") @PathVariable UUID athleteId,
			@Parameter(description = "Filtro opcional de status do treino", example = "ACTIVE")
			@RequestParam(required = false) TrainingStatus status) {
		requireCoachOwnsAthlete(jwt, athleteId);
		List<Training> trainings = (status != null)
				? this.queryService.findTrainingsByAthleteAndStatus(athleteId, status)
				: this.queryService.findTrainingsByAthlete(athleteId);
		return ResponseEntity.ok(trainings.stream().map(TrainingResponse::from).toList());
	}

	@Operation(summary = "Adicionar exercício ao treino",
			description = "Prescreve um exercício no treino (séries, repetições, carga, intensidade e descanso). "
					+ "Pode referenciar um exercício da biblioteca (exerciseId) ou um exercício personalizado "
					+ "do coach (customExercise=true + exerciseName).")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Exercício adicionado ao treino"),
			@ApiResponse(responseCode = "400", description = "Dados da prescrição inválidos"),
			@ApiResponse(responseCode = "404", description = "Treino não encontrado") })
	// T-009/US-037: somente o coach gerencia protocolos.
	// T-058 (anti-IDOR): o coach só prescreve em treinos de atleta da própria
	// carteira — o id de um treino de outro coach resulta em 404.
	@PreAuthorize("hasRole('COACH')")
	@PostMapping("/{id}/exercises")
	public ResponseEntity<TrainingResponse> addExercise(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Identificador do treino") @PathVariable UUID id,
			@RequestBody AddExerciseRequest request) {
		Training training = this.addExerciseUseCase.execute(id, UUID.fromString(jwt.getSubject()),
				request.exerciseId(), request.exerciseName(),
				request.customExercise(), request.orderIndex(), request.sets(), request.repetitions(), request.load(),
				request.intensityType(), request.intensityValue(), request.restSeconds(), request.notes());
		return ResponseEntity.ok(TrainingResponse.from(training));
	}

	@Operation(summary = "Reordenar exercícios do treino",
			description = "Redefine a ordem de execução dos exercícios do treino a partir da lista ordenada de "
					+ "ids de prescrição.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Exercícios reordenados"),
			@ApiResponse(responseCode = "400", description = "Lista de prescrições inválida ou incompleta"),
			@ApiResponse(responseCode = "404", description = "Treino não encontrado") })
	// T-009/US-037: somente o coach gerencia protocolos.
	// T-058 (anti-IDOR): o coach só reordena treinos de atleta da própria
	// carteira — o id de um treino de outro coach resulta em 404.
	@PreAuthorize("hasRole('COACH')")
	@PutMapping("/{id}/exercises/reorder")
	public ResponseEntity<TrainingResponse> reorderExercises(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Identificador do treino") @PathVariable UUID id,
			@RequestBody ReorderExercisesRequest request) {
		Training training = this.reorderExercisesUseCase.execute(id, UUID.fromString(jwt.getSubject()),
				request.orderedPrescriptionIds());
		return ResponseEntity.ok(TrainingResponse.from(training));
	}

	@Operation(summary = "Transicionar status do treino",
			description = "Avança o treino no fluxo DRAFT → IN_REVIEW → ACTIVE → FINISHED. Transições inválidas "
					+ "são rejeitadas com 409 Conflict.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Status alterado com sucesso"),
			@ApiResponse(responseCode = "404", description = "Treino não encontrado"),
			@ApiResponse(responseCode = "409", description = "Transição de status não permitida") })
	// T-009/US-037: somente o coach gerencia protocolos.
	// T-058 (anti-IDOR): o coach só transiciona treinos de atleta da própria
	// carteira — o id de um treino de outro coach resulta em 404.
	@PreAuthorize("hasRole('COACH')")
	@PostMapping("/{id}/transition")
	public ResponseEntity<TrainingResponse> transitionStatus(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Identificador do treino") @PathVariable UUID id,
			@RequestBody TransitionStatusRequest request) {
		Training training = this.transitionStatusUseCase.execute(id, UUID.fromString(jwt.getSubject()),
				request.targetStatus());
		return ResponseEntity.ok(TrainingResponse.from(training));
	}

	@Operation(summary = "Remover treino (soft-delete)",
			description = "Exclusão lógica do treino: o registro é apenas marcado como excluído e nunca removido "
					+ "fisicamente do banco.")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Treino removido (soft-delete)"),
			@ApiResponse(responseCode = "404", description = "Treino não encontrado") })
	// T-009/US-037: somente o coach gerencia protocolos.
	// T-058 (anti-IDOR): o coach só remove treinos de atleta da própria carteira —
	// o id de um treino de outro coach resulta em 404.
	@PreAuthorize("hasRole('COACH')")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> softDelete(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Identificador do treino") @PathVariable UUID id) {
		this.softDeleteUseCase.execute(id, UUID.fromString(jwt.getSubject()));
		return ResponseEntity.noContent().build();
	}

	/**
	 * Para chamadas de coach, garante que o atleta pertence à carteira do coach
	 * autenticado. Chamadas de atleta (self-access) já foram validadas pelo
	 * {@code @athleteGuard} e não passam por aqui.
	 */
	private void requireCoachOwnsAthlete(Jwt jwt, UUID athleteId) {
		if (jwt == null || !"COACH".equals(jwt.getClaimAsString("role"))) {
			return;
		}
		UUID coachId = coachService.requireCoachIdByUserId(UUID.fromString(jwt.getSubject())).value();
		athleteAccessService.requireOwnedByCoach(athleteId, coachId);
	}

}
