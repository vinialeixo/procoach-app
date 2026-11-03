package com.fitness.procoach.training.interfaces.rest;

import java.util.List;
import jakarta.validation.Valid;
import java.util.UUID;

import com.fitness.procoach.athlete.application.AthleteAccessService;
import com.fitness.procoach.coach.application.CoachApplicationService;
import com.fitness.procoach.training.application.CreatePeriodizationUseCase;
import com.fitness.procoach.training.application.TrainingQueryService;
import com.fitness.procoach.training.domain.model.Periodization;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
@RequestMapping("/api/training/periodizations")
public class PeriodizationController {

	private final CreatePeriodizationUseCase createPeriodizationUseCase;

	private final TrainingQueryService queryService;

	private final CoachApplicationService coachService;

	private final AthleteAccessService athleteAccessService;

	public PeriodizationController(CreatePeriodizationUseCase createPeriodizationUseCase,
			TrainingQueryService queryService, CoachApplicationService coachService,
			AthleteAccessService athleteAccessService) {
		this.createPeriodizationUseCase = createPeriodizationUseCase;
		this.queryService = queryService;
		this.coachService = coachService;
		this.athleteAccessService = athleteAccessService;
	}

	@Operation(summary = "Criar periodização",
			description = "Cria uma periodização (macrociclo) para o atleta, com nome, descrição e período "
					+ "(data de início/fim). Retorna 201 Created.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Periodização criada com sucesso"),
			@ApiResponse(responseCode = "400", description = "Dados inválidos") })
	// T-009/US-037: somente o coach gerencia protocolos.
	@PreAuthorize("hasRole('COACH')")
	@PostMapping
	public ResponseEntity<PeriodizationResponse> create(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CreatePeriodizationRequest request) {
		// T-058: o coachId nunca vem do request — é resolvido server-side a partir
		// do usuário autenticado (subject do JWT) na camada de aplicação.
		Periodization periodization = this.createPeriodizationUseCase.execute(request.athleteId(),
				UUID.fromString(jwt.getSubject()), request.name(), request.description(), request.startDate(),
				request.endDate());
		return ResponseEntity.status(HttpStatus.CREATED).body(PeriodizationResponse.from(periodization));
	}

	@Operation(summary = "Buscar periodização por id")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Periodização encontrada"),
			@ApiResponse(responseCode = "404", description = "Periodização não encontrada") })
	// T-009/T-010: acesso direto por id é restrito ao coach — o atleta usa
	// GET /athlete/{athleteId} (ancorado no claim do JWT).
	// T-058 (anti-IDOR): o coach só acessa periodização de atleta da própria
	// carteira — o id de um recurso de outro coach resulta em 404 (a guarda da
	// camada de aplicação lança AthleteNotFoundException).
	@PreAuthorize("hasRole('COACH')")
	@GetMapping("/{id}")
	public ResponseEntity<PeriodizationResponse> findById(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Identificador da periodização") @PathVariable UUID id) {
		return this.queryService.findPeriodizationById(id, UUID.fromString(jwt.getSubject()))
			.map(p -> ResponseEntity.ok(PeriodizationResponse.from(p)))
			.orElse(ResponseEntity.notFound().build());
	}

	@Operation(summary = "Listar periodizações do atleta",
			description = "Lista as periodizações de um atleta. O atleta só consulta os próprios dados "
					+ "(claim athleteId do JWT — US-038).")
	@ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
	// T-010/US-038: atleta só consulta os próprios dados (claim athleteId do JWT).
	// T-058: coach só consulta atletas da própria carteira.
	@PreAuthorize("@athleteGuard.canAccess(authentication, #athleteId)")
	@GetMapping("/athlete/{athleteId}")
	public ResponseEntity<List<PeriodizationResponse>> findByAthlete(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Identificador do atleta") @PathVariable UUID athleteId) {
		requireCoachOwnsAthlete(jwt, athleteId);
		List<PeriodizationResponse> result = this.queryService.findPeriodizationsByAthlete(athleteId, isAthlete(jwt))
			.stream()
			.map(PeriodizationResponse::from)
			.toList();
		return ResponseEntity.ok(result);
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

	/** Chamada do próprio atleta (somente leitura, só conteúdo liberado pelo coach). */
	private static boolean isAthlete(Jwt jwt) {
		return jwt != null && "ATHLETE".equals(jwt.getClaimAsString("role"));
	}
}
