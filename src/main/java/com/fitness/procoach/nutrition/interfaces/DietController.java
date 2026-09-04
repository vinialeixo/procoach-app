package com.fitness.procoach.nutrition.interfaces;

import com.fitness.procoach.athlete.application.AthleteAccessService;
import com.fitness.procoach.coach.application.CoachApplicationService;
import com.fitness.procoach.nutrition.application.DietApplicationService;
import com.fitness.procoach.nutrition.domain.Diet;
import com.fitness.procoach.nutrition.domain.DietId;
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
 * Controller REST de dietas (US-007). Cria a primeira versão (v1) e consulta a
 * versão mais recente ou o histórico de versões de um atleta.
 */
@Tag(name = "Nutrition",
		description = "Gestão e versionamento de dietas. Regra inegociável de domínio (US-015): "
				+ "uma dieta NUNCA é sobrescrita — não existe endpoint de update (PUT/PATCH). "
				+ "Toda alteração cria uma nova versão encadeada (v1→v2→v3) via fluxo de versionamento "
				+ "POST /api/v1/diets/{dietId}/versions, mantendo referência à versão anterior "
				+ "(previousVersionId), que passa a ser somente leitura (readOnly=true).")
@RestController
@RequestMapping("/api/v1/diets")
public class DietController {

	private final DietApplicationService service;
	private final CoachApplicationService coachService;
	private final AthleteAccessService athleteAccessService;

	public DietController(DietApplicationService service, CoachApplicationService coachService,
			AthleteAccessService athleteAccessService) {
		this.service = service;
		this.coachService = coachService;
		this.athleteAccessService = athleteAccessService;
	}

	/**
	 * Cria a primeira versão (v1) de uma dieta. Retorna {@code 201 Created} com
	 * o DTO no corpo e o header {@code Location} apontando para o recurso.
	 */
	@Operation(summary = "Criar dieta (v1)",
			description = "Cria a primeira versão (v1) de uma dieta em estado Rascunho. Retorna 201 Created com o "
					+ "header Location do recurso. Atenção (US-015): este recurso nunca será sobrescrito — "
					+ "alterações posteriores devem gerar uma nova versão (POST /api/v1/diets/{dietId}/versions).")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Dieta criada com sucesso"),
			@ApiResponse(responseCode = "400", description = "Campos inválidos ou ausentes") })
	// T-009/US-037: somente o coach monta/edita protocolos.
	@PreAuthorize("hasRole('COACH')")
	@PostMapping
	public ResponseEntity<DietResponse> create(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CreateDietRequest request) {
		// T-058: o coachId nunca vem do request — é resolvido server-side a
		// partir do usuário autenticado (subject do JWT) na camada de aplicação.
		Diet diet = service.createDiet(
				new DietApplicationService.CreateDietCommand(
						UUID.fromString(jwt.getSubject()), request.athleteId(), request.objective(),
						request.calories(), request.protein(), request.carbohydrates(),
						request.fat(), request.createdBy()));
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(diet.id().value())
				.toUri();
		return ResponseEntity.created(location).body(DietResponse.from(diet));
	}

	/**
	 * Retorna uma dieta pelo identificador.
	 */
	@Operation(summary = "Buscar dieta por id",
			description = "Retorna uma versão específica da dieta pelo identificador. Versões anteriores são "
					+ "imutáveis (readOnly=true) e nunca sobrescritas (US-015).")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Dieta encontrada"),
			@ApiResponse(responseCode = "404", description = "Dieta não encontrada") })
	// T-009/T-010: acesso direto por id é restrito ao coach — o atleta consulta
	// a própria dieta pelos endpoints com ?athleteId= (ancorados no claim do JWT).
	@PreAuthorize("hasRole('COACH')")
	@GetMapping("/{id}")
	public DietResponse findById(@Parameter(description = "Identificador da dieta") @PathVariable UUID id) {
		return DietResponse.from(service.findById(new DietId(id)));
	}

	/**
	 * Versão mais recente da dieta de um atleta (US-007).
	 */
	@Operation(summary = "Versão mais recente da dieta",
			description = "Retorna a versão mais recente da dieta de um atleta (US-007).")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Versão mais recente retornada"),
			@ApiResponse(responseCode = "404", description = "Nenhuma dieta encontrada para o atleta") })
	// T-010/US-038: atleta só consulta os próprios dados (claim athleteId do JWT).
	// T-058: coach só consulta atletas da própria carteira — o athleteId de outro
	// coach resulta em 404 (não vaza a existência do recurso).
	@PreAuthorize("@athleteGuard.canAccess(authentication, #athleteId)")
	@GetMapping
	public DietResponse latest(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Identificador do atleta") @RequestParam UUID athleteId) {
		requireCoachOwnsAthlete(jwt, athleteId);
		return DietResponse.from(service.latestByAthlete(athleteId));
	}

	/**
	 * Histórico de versões da dieta de um atleta.
	 */
	@Operation(summary = "Histórico de versões da dieta",
			description = "Retorna o histórico encadeado de versões da dieta de um atleta (v1→v2→v3), evidência "
					+ "da regra de nunca sobrescrever (US-015): cada item aponta para previousVersionId.")
	@ApiResponse(responseCode = "200", description = "Histórico retornado com sucesso")
	// T-010/US-038: atleta só consulta os próprios dados (claim athleteId do JWT).
	// T-058: coach só consulta atletas da própria carteira.
	@PreAuthorize("@athleteGuard.canAccess(authentication, #athleteId)")
	@GetMapping("/history")
	public List<DietResponse> history(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Identificador do atleta") @RequestParam UUID athleteId) {
		requireCoachOwnsAthlete(jwt, athleteId);
		return service.historyByAthlete(athleteId).stream().map(DietResponse::from).toList();
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
