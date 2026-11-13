package com.fitness.procoach.nutrition.interfaces;

import com.fitness.procoach.assessment.application.AssessmentApplicationService;
import com.fitness.procoach.athlete.application.AthleteAccessService;
import com.fitness.procoach.coach.application.CoachApplicationService;
import com.fitness.procoach.nutrition.application.DietApplicationService;
import com.fitness.procoach.nutrition.domain.Diet;
import com.fitness.procoach.nutrition.domain.DietId;
import com.fitness.procoach.nutrition.domain.FoodKind;
import com.fitness.procoach.nutrition.domain.FoodSource;
import com.fitness.procoach.nutrition.domain.FoodState;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;
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
	private final AssessmentApplicationService assessmentService;

	public DietController(DietApplicationService service, CoachApplicationService coachService,
			AthleteAccessService athleteAccessService, AssessmentApplicationService assessmentService) {
		this.service = service;
		this.coachService = coachService;
		this.athleteAccessService = athleteAccessService;
		this.assessmentService = assessmentService;
	}

	/**
	 * Obtém o peso do atleta da avaliação mais recente (para cálculos derivados g/kg).
	 */
	private double getAthleteWeightKg(UUID athleteId) {
		return assessmentService.latestEnergyProfile(athleteId)
				.map(p -> p.weightKg().doubleValue())
				.orElse(0.0);
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
		// T-058: o coachId e o createdBy (auditoria) nunca vêm do request — são
		// resolvidos server-side a partir do usuário autenticado (subject do JWT).
		Diet diet = service.createDiet(
				new DietApplicationService.CreateDietCommand(
						UUID.fromString(jwt.getSubject()), request.athleteId(), request.objective(),
						request.calories(), request.protein(), request.carbohydrates(),
						request.fat()));
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(diet.id().value())
				.toUri();
		double weightKg = getAthleteWeightKg(diet.athleteId().value());
		return ResponseEntity.created(location).body(DietResponse.from(diet, weightKg));
	}

	@Operation(summary = "Criar nova versão da dieta",
			description = "Cria a próxima versão (v2, v3...) a partir da versão informada. A versão de origem "
					+ "passa a ser somente leitura e o motivo da alteração é obrigatório (US-015/US-016).")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Nova versão criada"),
			@ApiResponse(responseCode = "400", description = "Campos inválidos ou motivo ausente"),
			@ApiResponse(responseCode = "404", description = "Dieta não encontrada ou de outro coach"),
			@ApiResponse(responseCode = "409", description = "Versão de origem somente leitura ou removida") })
	@PreAuthorize("hasRole('COACH')")
	@PostMapping("/{id}/versions")
	public ResponseEntity<DietResponse> createVersion(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Versão de origem") @PathVariable UUID id,
			@Valid @RequestBody CreateDietVersionRequest request) {
		Diet next = service.createNewVersion(new DietApplicationService.NewVersionCommand(
				UUID.fromString(jwt.getSubject()), id, request.objective(), request.calories(),
				request.protein(), request.carbohydrates(), request.fat(), request.changeReason()));
		URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
				.path("/api/v1/diets/{id}")
				.buildAndExpand(next.id().value())
				.toUri();
		double weightKg = getAthleteWeightKg(next.athleteId().value());
		return ResponseEntity.created(location).body(DietResponse.from(next, weightKg));
	}

	@Operation(summary = "Alterar status da dieta",
			description = "Fluxo Rascunho → Em Revisão → Aprovada → Arquivada (US-017). Transição inválida retorna 409.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Status alterado"),
			@ApiResponse(responseCode = "404", description = "Dieta não encontrada ou de outro coach"),
			@ApiResponse(responseCode = "409", description = "Transição inválida") })
	@PreAuthorize("hasRole('COACH')")
	@PostMapping("/{id}/status")
	public DietResponse changeStatus(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Identificador da dieta") @PathVariable UUID id,
			@Valid @RequestBody ChangeDietStatusRequest request) {
		Diet diet = service.changeStatus(UUID.fromString(jwt.getSubject()), id, request.status());
		double weightKg = getAthleteWeightKg(diet.athleteId().value());
		return DietResponse.from(diet, weightKg);
	}

	@Operation(summary = "Balanço energético da dieta atual do atleta",
			description = "Balanço (déficit/manutenção/superávit) da versão mais recente da dieta do atleta, "
					+ "contra o GET da avaliação mais recente. O atleta consulta o próprio balanço (claim athleteId "
					+ "do JWT); o coach, o de atletas da própria carteira. 404 sem dieta; 409 sem avaliação.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Balanço calculado"),
			@ApiResponse(responseCode = "403", description = "Atleta consultando dados de outro atleta"),
			@ApiResponse(responseCode = "404", description = "Atleta sem dieta, ou atleta de outro coach"),
			@ApiResponse(responseCode = "409", description = "Atleta sem avaliação para basear o cálculo") })
	// T-010/US-038: atleta só consulta os próprios dados (claim athleteId do JWT).
	// T-058: coach só consulta atletas da própria carteira.
	@PreAuthorize("@athleteGuard.canAccess(authentication, #athleteId)")
	@GetMapping("/energy-balance")
	public EnergyBalanceResponse athleteEnergyBalance(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Identificador do atleta") @RequestParam UUID athleteId) {
		requireCoachOwnsAthlete(jwt, athleteId);
		return EnergyBalanceResponse.from(service.latestEnergyBalance(athleteId, isAthlete(jwt)));
	}

	@Operation(summary = "Balanço energético da dieta",
			description = "Compara as calorias da dieta com o GET da avaliação mais recente do atleta e informa "
					+ "déficit, manutenção (±5% do GET) ou superávit (T-030/US-07). Atleta sem avaliação retorna 409.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Balanço calculado"),
			@ApiResponse(responseCode = "404", description = "Dieta não encontrada ou de outro coach"),
			@ApiResponse(responseCode = "409", description = "Atleta sem avaliação para basear o cálculo") })
	@PreAuthorize("hasRole('COACH')")
	@GetMapping("/{id}/energy-balance")
	public EnergyBalanceResponse energyBalance(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Identificador da dieta") @PathVariable UUID id) {
		return EnergyBalanceResponse.from(service.energyBalance(id, UUID.fromString(jwt.getSubject())));
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
	// T-058 (anti-IDOR): o coach só acessa dieta de atleta da própria carteira —
	// o id de um recurso de outro coach resulta em 404 (não vaza a existência).
	@PreAuthorize("hasRole('COACH')")
	@GetMapping("/{id}")
	public DietResponse findById(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Identificador da dieta") @PathVariable UUID id) {
		Diet diet = service.findOwnedById(new DietId(id), UUID.fromString(jwt.getSubject()));
		double weightKg = getAthleteWeightKg(diet.athleteId().value());
		return DietResponse.from(diet, weightKg);
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
		Diet diet = service.latestByAthlete(athleteId, isAthlete(jwt));
		double weightKg = getAthleteWeightKg(athleteId);
		return DietResponse.from(diet, weightKg);
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
		double weightKg = getAthleteWeightKg(athleteId);
		return service.historyByAthlete(athleteId, isAthlete(jwt)).stream()
				.map(d -> DietResponse.from(d, weightKg))
				.toList();
	}

	// ===== T-085: Meal Management Endpoints =====

	@Operation(summary = "Adicionar refeição à dieta",
			description = "Adiciona uma refeição a uma versão da dieta (T-085). Apenas versões não read-only (DRAFT).")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Refeição adicionada"),
			@ApiResponse(responseCode = "404", description = "Dieta não encontrada ou de outro coach"),
			@ApiResponse(responseCode = "409", description = "Dieta somente leitura ou removida") })
	@PreAuthorize("hasRole('COACH')")
	@PostMapping("/{id}/meals")
	public ResponseEntity<DietResponse> addMeal(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Identificador da dieta") @PathVariable UUID id,
			@Valid @RequestBody AddMealRequest request) {
		Diet diet = service.addMeal(UUID.fromString(jwt.getSubject()), id,
				new DietApplicationService.AddMealCommand(UUID.fromString(jwt.getSubject()), id,
						request.name(), request.order(), request.time(), request.notes(),
						request.target() != null ? new DietApplicationService.MealTarget(
								request.target().calories(), request.target().protein(),
								request.target().carbohydrates(), request.target().fat()) : null));
		URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
				.path("/api/v1/diets/{id}")
				.buildAndExpand(diet.id().value())
				.toUri();
		double weightKg = getAthleteWeightKg(diet.athleteId().value());
		return ResponseEntity.created(location).body(DietResponse.from(diet, weightKg));
	}

	@Operation(summary = "Adicionar item a uma refeição",
			description = "Adiciona um item de alimento a uma refeição da dieta (T-085). Macros são snapshot no momento da prescrição.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Item adicionado"),
			@ApiResponse(responseCode = "404", description = "Dieta ou refeição não encontrada"),
			@ApiResponse(responseCode = "409", description = "Dieta somente leitura ou removida") })
	@PreAuthorize("hasRole('COACH')")
	@PostMapping("/{id}/meals/{mealId}/items")
	public DietResponse addMealItem(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Identificador da dieta") @PathVariable UUID id,
			@Parameter(description = "Identificador da refeição") @PathVariable UUID mealId,
			@Valid @RequestBody AddMealItemRequest request) {
		Diet diet = service.addMealItem(UUID.fromString(jwt.getSubject()), id, mealId,
				new DietApplicationService.AddMealItemCommand(UUID.fromString(jwt.getSubject()), id, mealId,
						request.foodId(), request.name(), request.source(), request.kind(),
						request.state(), request.quantityG(), request.householdMeasure(),
						request.preparation(), request.calories(), request.protein(),
						request.carbohydrates(), request.fat()));
		double weightKg = getAthleteWeightKg(diet.athleteId().value());
		return DietResponse.from(diet, weightKg);
	}

	@Operation(summary = "Remover item de uma refeição",
			description = "Remove um item de uma refeição da dieta (T-085). Recalcula os totais automaticamente.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Item removido"),
			@ApiResponse(responseCode = "404", description = "Dieta, refeição ou item não encontrado"),
			@ApiResponse(responseCode = "409", description = "Dieta somente leitura ou removida") })
	@PreAuthorize("hasRole('COACH')")
	@DeleteMapping("/{id}/meals/{mealId}/items/{itemId}")
	public DietResponse removeMealItem(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Identificador da dieta") @PathVariable UUID id,
			@Parameter(description = "Identificador da refeição") @PathVariable UUID mealId,
			@Parameter(description = "Identificador do item") @PathVariable UUID itemId) {
		Diet diet = service.removeMealItem(UUID.fromString(jwt.getSubject()), id, mealId, itemId);
		double weightKg = getAthleteWeightKg(diet.athleteId().value());
		return DietResponse.from(diet, weightKg);
	}

	@Operation(summary = "Remover refeição da dieta",
			description = "Remove uma refeição e todos os seus itens da dieta (T-085). Recalcula os totais automaticamente.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Refeição removida"),
			@ApiResponse(responseCode = "404", description = "Dieta ou refeição não encontrada"),
			@ApiResponse(responseCode = "409", description = "Dieta somente leitura ou removida") })
	@PreAuthorize("hasRole('COACH')")
	@DeleteMapping("/{id}/meals/{mealId}")
	public DietResponse removeMeal(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Identificador da dieta") @PathVariable UUID id,
			@Parameter(description = "Identificador da refeição") @PathVariable UUID mealId) {
		Diet diet = service.removeMeal(UUID.fromString(jwt.getSubject()), id, mealId);
		double weightKg = getAthleteWeightKg(diet.athleteId().value());
		return DietResponse.from(diet, weightKg);
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
