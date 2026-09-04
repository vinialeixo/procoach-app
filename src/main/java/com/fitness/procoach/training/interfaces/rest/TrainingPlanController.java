package com.fitness.procoach.training.interfaces.rest;

import java.util.List;
import java.util.UUID;

import com.fitness.procoach.athlete.application.AthleteAccessService;
import com.fitness.procoach.coach.application.CoachApplicationService;
import com.fitness.procoach.training.application.CreateTrainingPlanUseCase;
import com.fitness.procoach.training.application.CreateTrainingPlanVersionUseCase;
import com.fitness.procoach.training.application.SoftDeleteTrainingPlanUseCase;
import com.fitness.procoach.training.application.TrainingQueryService;
import com.fitness.procoach.training.application.TransitionTrainingPlanStatusUseCase;
import com.fitness.procoach.training.domain.model.TrainingPlan;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
@RequestMapping("/api/training/plans")
public class TrainingPlanController {

	private final CreateTrainingPlanUseCase createTrainingPlanUseCase;

	private final CreateTrainingPlanVersionUseCase createVersionUseCase;

	private final TransitionTrainingPlanStatusUseCase transitionStatusUseCase;

	private final SoftDeleteTrainingPlanUseCase softDeleteUseCase;

	private final TrainingQueryService queryService;

	private final CoachApplicationService coachService;

	private final AthleteAccessService athleteAccessService;

	public TrainingPlanController(CreateTrainingPlanUseCase createTrainingPlanUseCase,
			CreateTrainingPlanVersionUseCase createVersionUseCase,
			TransitionTrainingPlanStatusUseCase transitionStatusUseCase,
			SoftDeleteTrainingPlanUseCase softDeleteUseCase, TrainingQueryService queryService,
			CoachApplicationService coachService, AthleteAccessService athleteAccessService) {
		this.createTrainingPlanUseCase = createTrainingPlanUseCase;
		this.createVersionUseCase = createVersionUseCase;
		this.transitionStatusUseCase = transitionStatusUseCase;
		this.softDeleteUseCase = softDeleteUseCase;
		this.queryService = queryService;
		this.coachService = coachService;
		this.athleteAccessService = athleteAccessService;
	}

	@Operation(summary = "Criar plano de treino (v1)",
			description = "Cria a primeira versão (v1) de um plano de treino do atleta. Retorna 201 Created. "
					+ "Atenção: o plano nunca é sobrescrito — alterações posteriores devem gerar uma nova versão "
					+ "(POST /{id}/versions).")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Plano de treino criado com sucesso"),
			@ApiResponse(responseCode = "400", description = "Dados inválidos") })
	// T-009/US-037: somente o coach gerencia protocolos.
	@PreAuthorize("hasRole('COACH')")
	@PostMapping
	public ResponseEntity<TrainingPlanResponse> create(@AuthenticationPrincipal Jwt jwt,
			@RequestBody CreateTrainingPlanRequest request) {
		// T-058: o coachId nunca vem do request — é resolvido server-side a partir
		// do usuário autenticado (subject do JWT) na camada de aplicação.
		TrainingPlan plan = this.createTrainingPlanUseCase.execute(request.athleteId(),
				UUID.fromString(jwt.getSubject()), request.periodizationId(), request.name(), request.description());
		return ResponseEntity.status(HttpStatus.CREATED).body(TrainingPlanResponse.from(plan));
	}

	@Operation(summary = "Buscar plano de treino por id",
			description = "Retorna uma versão específica do plano. Versões anteriores são imutáveis e nunca "
					+ "sobrescritas.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Plano encontrado"),
			@ApiResponse(responseCode = "404", description = "Plano não encontrado") })
	// T-009/T-010: acesso direto por id é restrito ao coach — o atleta usa os
	// endpoints /athlete/{athleteId} (ancorados no claim do JWT).
	@PreAuthorize("hasRole('COACH')")
	@GetMapping("/{id}")
	public ResponseEntity<TrainingPlanResponse> findById(
			@Parameter(description = "Identificador do plano") @PathVariable UUID id) {
		return this.queryService.findTrainingPlanById(id)
			.map(tp -> ResponseEntity.ok(TrainingPlanResponse.from(tp)))
			.orElse(ResponseEntity.notFound().build());
	}

	@Operation(summary = "Listar planos do atleta",
			description = "Lista todas as versões de planos de treino de um atleta. O atleta só consulta os "
					+ "próprios dados (claim athleteId do JWT — US-038).")
	@ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
	// T-010/US-038: atleta só consulta os próprios dados (claim athleteId do JWT).
	// T-058: coach só consulta atletas da própria carteira.
	@PreAuthorize("@athleteGuard.canAccess(authentication, #athleteId)")
	@GetMapping("/athlete/{athleteId}")
	public ResponseEntity<List<TrainingPlanResponse>> findByAthlete(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Identificador do atleta") @PathVariable UUID athleteId) {
		requireCoachOwnsAthlete(jwt, athleteId);
		List<TrainingPlanResponse> result = this.queryService.findTrainingPlansByAthlete(athleteId)
			.stream()
			.map(TrainingPlanResponse::from)
			.toList();
		return ResponseEntity.ok(result);
	}

	@Operation(summary = "Versão mais recente do plano do atleta",
			description = "Retorna a versão mais recente do plano de treino do atleta. O atleta só consulta os "
					+ "próprios dados (claim athleteId do JWT — US-038).")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Versão mais recente retornada"),
			@ApiResponse(responseCode = "404", description = "Nenhum plano encontrado para o atleta") })
	// T-010/US-038: atleta só consulta os próprios dados (claim athleteId do JWT).
	// T-058: coach só consulta atletas da própria carteira.
	@PreAuthorize("@athleteGuard.canAccess(authentication, #athleteId)")
	@GetMapping("/athlete/{athleteId}/latest")
	public ResponseEntity<TrainingPlanResponse> findLatestVersion(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Identificador do atleta") @PathVariable UUID athleteId) {
		requireCoachOwnsAthlete(jwt, athleteId);
		return this.queryService.findLatestPlanVersion(athleteId)
			.map(tp -> ResponseEntity.ok(TrainingPlanResponse.from(tp)))
			.orElse(ResponseEntity.notFound().build());
	}

	@Operation(summary = "Criar nova versão do plano",
			description = "Fluxo de versionamento sem sobrescrita: cria uma nova versão (vN+1) do plano a partir "
					+ "da versão {id}, que permanece intacta e passa a ser somente leitura. A nova versão mantém "
					+ "referência à anterior (previousVersionId), formando o histórico encadeado. Retorna 201 "
					+ "Created.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Nova versão criada com sucesso"),
			@ApiResponse(responseCode = "404", description = "Plano não encontrado"),
			@ApiResponse(responseCode = "409", description = "Versão de origem não permite edição (ex.: arquivada)") })
	// T-009/US-037: somente o coach gerencia protocolos.
	@PreAuthorize("hasRole('COACH')")
	@PostMapping("/{id}/versions")
	public ResponseEntity<TrainingPlanResponse> createNewVersion(
			@Parameter(description = "Identificador da versão de origem do plano") @PathVariable UUID id,
			@RequestBody CreateTrainingPlanVersionRequest request) {
		TrainingPlan newVersion = this.createVersionUseCase.execute(id, request.newName(), request.newDescription());
		return ResponseEntity.status(HttpStatus.CREATED).body(TrainingPlanResponse.from(newVersion));
	}

	@Operation(summary = "Transicionar status do plano",
			description = "Avança o plano no fluxo DRAFT → IN_REVIEW → ACTIVE → FINISHED. Transições inválidas "
					+ "são rejeitadas com 409 Conflict.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Status alterado com sucesso"),
			@ApiResponse(responseCode = "404", description = "Plano não encontrado"),
			@ApiResponse(responseCode = "409", description = "Transição de status não permitida") })
	// T-009/US-037: somente o coach gerencia protocolos.
	@PreAuthorize("hasRole('COACH')")
	@PostMapping("/{id}/transition")
	public ResponseEntity<TrainingPlanResponse> transitionStatus(
			@Parameter(description = "Identificador do plano") @PathVariable UUID id,
			@RequestBody TransitionStatusRequest request) {
		TrainingPlan plan = this.transitionStatusUseCase.execute(id, request.targetStatus());
		return ResponseEntity.ok(TrainingPlanResponse.from(plan));
	}

	@Operation(summary = "Remover plano (soft-delete)",
			description = "Exclusão lógica do plano de treino: o registro é apenas marcado como excluído e nunca "
					+ "removido fisicamente do banco.")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Plano removido (soft-delete)"),
			@ApiResponse(responseCode = "404", description = "Plano não encontrado") })
	// T-009/US-037: somente o coach gerencia protocolos.
	@PreAuthorize("hasRole('COACH')")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> softDelete(@Parameter(description = "Identificador do plano") @PathVariable UUID id) {
		this.softDeleteUseCase.execute(id);
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
