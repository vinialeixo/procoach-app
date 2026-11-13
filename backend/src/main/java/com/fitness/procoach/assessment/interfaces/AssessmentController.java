package com.fitness.procoach.assessment.interfaces;

import com.fitness.procoach.assessment.application.AssessmentApplicationService;
import com.fitness.procoach.assessment.domain.Assessment;
import com.fitness.procoach.assessment.domain.AssessmentId;
import com.fitness.procoach.athlete.application.AthleteAccessService;
import com.fitness.procoach.coach.application.CoachApplicationService;
import com.fitness.procoach.security.application.CoachAthleteAccessGuard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
@Tag(name = "Assessment",
		description = "Avaliações físicas do atleta. Cada avaliação é um novo registro imutável — "
				+ "avaliações anteriores nunca são sobrescritas, preservando o histórico de evolução.")
@RestController
@RequestMapping("/api/v1/assessments")
public class AssessmentController {

	private final AssessmentApplicationService service;
	private final CoachApplicationService coachService;
	private final AthleteAccessService athleteAccessService;
	private final CoachAthleteAccessGuard coachAthleteGuard;

	public AssessmentController(AssessmentApplicationService service, CoachApplicationService coachService,
			AthleteAccessService athleteAccessService, CoachAthleteAccessGuard coachAthleteGuard) {
		this.service = service;
		this.coachService = coachService;
		this.athleteAccessService = athleteAccessService;
		this.coachAthleteGuard = coachAthleteGuard;
	}

	/**
	 * Registra uma nova avaliação. Retorna {@code 201 Created} com o DTO da
	 * avaliação no corpo e o header {@code Location} apontando para o recurso
	 * criado. Cada avaliação é um novo registro — avaliações anteriores nunca
	 * são sobrescritas.
	 */
	@Operation(summary = "Registrar avaliação",
			description = "Registra uma nova avaliação física. Retorna 201 Created com o header Location do recurso "
					+ "criado. Cada avaliação é um novo registro — avaliações anteriores nunca são sobrescritas.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Avaliação registrada com sucesso"),
			@ApiResponse(responseCode = "400", description = "Campos inválidos ou ausentes") })
	// T-009/US-037: somente o coach registra avaliações.
	@PreAuthorize("hasRole('COACH')")
	@PostMapping
	public ResponseEntity<AssessmentResponse> register(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody RegisterAssessmentRequest request) {
		// T-058: o coachId nunca vem do request — é resolvido server-side a
		// partir do usuário autenticado (subject do JWT) na camada de aplicação.
		Assessment assessment = service.registerAssessment(
				new AssessmentApplicationService.RegisterAssessmentCommand(
						UUID.fromString(jwt.getSubject()), request.athleteId(), request.assessmentDate(),
						request.weightKg(), request.heightCm(), request.bodyFatPercentage(),
						request.muscleMassKg(), request.waistCm(), request.hipCm()));
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(assessment.id().value())
				.toUri();
		return ResponseEntity.created(location).body(AssessmentResponse.from(service.withMetrics(assessment)));
	}

	/**
	 * Retorna uma avaliação pelo identificador.
	 */
	@Operation(summary = "Buscar avaliação por id")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Avaliação encontrada"),
			@ApiResponse(responseCode = "404", description = "Avaliação não encontrada") })
	// T-009/T-010: acesso direto por id é restrito ao coach — o atleta consulta o
	// próprio histórico por GET /api/v1/assessments?athleteId= (ancorado no JWT).
	// T-058 (anti-IDOR): o coach só acessa avaliação de atleta da própria carteira —
	// o id de um recurso de outro coach resulta em 404 (não vaza a existência).
	@PreAuthorize("hasRole('COACH')")
	@GetMapping("/{id}")
	public AssessmentResponse findById(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Identificador da avaliação") @PathVariable UUID id) {
		return AssessmentResponse.from(service.withMetrics(
				service.findOwnedById(new AssessmentId(id), UUID.fromString(jwt.getSubject()))));
	}

	/**
	 * Histórico de avaliações de um atleta em ordem cronológica (US-004).
	 */
	@Operation(summary = "Histórico de avaliações",
			description = "Retorna o histórico de avaliações de um atleta em ordem cronológica (US-004).")
	@ApiResponse(responseCode = "200", description = "Histórico retornado com sucesso")
	// T-010/US-038: atleta só consulta os próprios dados (claim athleteId do JWT).
	// T-058: coach só consulta atletas da própria carteira — o athleteId de outro
	// coach resulta em 404 (não vaza a existência do recurso).
	@PreAuthorize("@athleteGuard.canAccess(authentication, #athleteId)")
	@GetMapping
	public List<AssessmentResponse> history(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Identificador do atleta") @RequestParam UUID athleteId) {
		coachAthleteGuard.requireCoachOwnsAthlete(jwt, athleteId);
		return service.historyWithMetrics(athleteId).stream().map(AssessmentResponse::from).toList();
	}

	/**
	 * Comparação lado a lado de duas avaliações do atleta (T-026/US-06).
	 */
	@Operation(summary = "Comparar duas avaliações",
			description = "Compara duas avaliações do mesmo atleta lado a lado, com a variação (to - from) de "
					+ "peso, gordura, massa muscular, cintura, quadril, IMC, TMB e GET (US-06).")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Comparação calculada"),
			@ApiResponse(responseCode = "400", description = "As duas avaliações são a mesma"),
			@ApiResponse(responseCode = "404", description = "Avaliação ou atleta não encontrado") })
	// Mesma regra do histórico: atleta só compara as próprias avaliações; coach só da carteira.
	@PreAuthorize("@athleteGuard.canAccess(authentication, #athleteId)")
	@GetMapping("/compare")
	public AssessmentComparisonResponse compare(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Identificador do atleta") @RequestParam UUID athleteId,
			@Parameter(description = "Avaliação de partida") @RequestParam UUID from,
			@Parameter(description = "Avaliação de chegada") @RequestParam UUID to) {
		coachAthleteGuard.requireCoachOwnsAthlete(jwt, athleteId);
		return AssessmentComparisonResponse.from(
				service.compare(athleteId, new AssessmentId(from), new AssessmentId(to)));
	}
}
