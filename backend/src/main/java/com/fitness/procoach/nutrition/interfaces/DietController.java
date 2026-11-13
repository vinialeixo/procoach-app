package com.fitness.procoach.nutrition.interfaces;

import com.fitness.procoach.assessment.application.AssessmentApplicationService;
import com.fitness.procoach.athlete.application.AthleteAccessService;
import com.fitness.procoach.coach.application.CoachApplicationService;
import com.fitness.procoach.coach.domain.CoachId;
import com.fitness.procoach.nutrition.application.DietApplicationService;
import com.fitness.procoach.nutrition.domain.AiPlanProposal;
import com.fitness.procoach.nutrition.domain.AthleteId;
import com.fitness.procoach.nutrition.domain.Diet;
import com.fitness.procoach.nutrition.domain.DietId;
import com.fitness.procoach.nutrition.domain.EnergyBalance;
import com.fitness.procoach.nutrition.domain.EnergyBalanceCalculator;
import com.fitness.procoach.nutrition.domain.DietObjective;
import com.fitness.procoach.nutrition.domain.DietStatus;
import com.fitness.procoach.nutrition.domain.DietWarning;
import com.fitness.procoach.nutrition.domain.FoodKind;
import com.fitness.procoach.nutrition.domain.FoodSource;
import com.fitness.procoach.nutrition.domain.FoodState;
import com.fitness.procoach.nutrition.domain.Macronutrients;
import com.fitness.procoach.nutrition.interfaces.AddSupplementRequest;
import com.fitness.procoach.nutrition.interfaces.AiPlanProposalRequest;
import com.fitness.procoach.nutrition.interfaces.AiPlanProposalResponse;
import com.fitness.procoach.security.application.CoachAthleteAccessGuard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
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
	private final CoachAthleteAccessGuard coachAthleteGuard;

	public DietController(DietApplicationService service, CoachApplicationService coachService,
			AthleteAccessService athleteAccessService, AssessmentApplicationService assessmentService,
			CoachAthleteAccessGuard coachAthleteGuard) {
		this.service = service;
		this.coachService = coachService;
		this.athleteAccessService = athleteAccessService;
		this.assessmentService = assessmentService;
		this.coachAthleteGuard = coachAthleteGuard;
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
			description = "Cria a primeira versão (v1) de uma dieta em estado Rascunho com balanço energético (T-089). "
					+ "Retorna 201 Created com o header Location do recurso. Atenção (US-015): este recurso nunca será "
					+ "sobrescrito — alterações posteriores devem gerar uma nova versão (POST /api/v1/diets/{dietId}/versions).")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Dieta criada com sucesso"),
			@ApiResponse(responseCode = "400", description = "Campos inválidos ou ausentes"),
			@ApiResponse(responseCode = "409", description = "Atleta sem avaliação (energyBalance virá null)") })
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
		
		// Tenta calcular balanço energético se houver avaliação
		EnergyBalance balance = null;
		AssessmentApplicationService.EnergyProfile profile = assessmentService
				.latestEnergyProfile(diet.athleteId().value())
				.orElse(null);
		if (profile != null) {
			balance = EnergyBalanceCalculator.calculate(diet.macros().calories(), profile.getKcal());
		}
		
		return ResponseEntity.created(location).body(DietResponse.from(diet, weightKg, balance));
	}

	@Operation(summary = "Criar nova versão da dieta",
			description = "Cria a próxima versão (v2, v3...) a partir da versão informada com balanço energético (T-089). "
					+ "A versão de origem passa a ser somente leitura e o motivo da alteração é obrigatório (US-015/US-016).")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Nova versão criada"),
			@ApiResponse(responseCode = "400", description = "Campos inválidos ou motivo ausente"),
			@ApiResponse(responseCode = "404", description = "Dieta não encontrada ou de outro coach"),
			@ApiResponse(responseCode = "409", description = "Versão de origem somente leitura ou removida; ou atleta sem avaliação (energyBalance virá null)") })
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
		
		// Tenta calcular balanço energético se houver avaliação
		EnergyBalance balance = null;
		AssessmentApplicationService.EnergyProfile profile = assessmentService
				.latestEnergyProfile(next.athleteId().value())
				.orElse(null);
		if (profile != null) {
			balance = EnergyBalanceCalculator.calculate(next.macros().calories(), profile.getKcal());
		}
		
		return ResponseEntity.created(location).body(DietResponse.from(next, weightKg, balance));
	}

	@Operation(summary = "Alterar status da dieta",
			description = "Fluxo Rascunho → Em Revisão → Aprovada → Arquivada (US-017). Transição inválida retorna 409. "
					+ "Retorna a dieta com balanço energético atualizado (T-089).")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Status alterado"),
			@ApiResponse(responseCode = "404", description = "Dieta não encontrada ou de outro coach"),
			@ApiResponse(responseCode = "409", description = "Transição inválida; ou atleta sem avaliação (energyBalance virá null)") })
	@PreAuthorize("hasRole('COACH')")
	@PostMapping("/{id}/status")
	public DietResponse changeStatus(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Identificador da dieta") @PathVariable UUID id,
			@Valid @RequestBody ChangeDietStatusRequest request) {
		Diet diet = service.changeStatus(UUID.fromString(jwt.getSubject()), id, request.status());
		double weightKg = getAthleteWeightKg(diet.athleteId().value());
		
		// Tenta calcular balanço energético se houver avaliação
		EnergyBalance balance = null;
		AssessmentApplicationService.EnergyProfile profile = assessmentService
				.latestEnergyProfile(diet.athleteId().value())
				.orElse(null);
		if (profile != null) {
			balance = EnergyBalanceCalculator.calculate(diet.macros().calories(), profile.getKcal());
		}
		
		return DietResponse.from(diet, weightKg, balance);
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
		coachAthleteGuard.requireCoachOwnsAthlete(jwt, athleteId);
		return EnergyBalanceResponse.from(service.latestEnergyBalance(athleteId, CoachAthleteAccessGuard.isAthlete(jwt)));
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
	 * Retorna uma dieta pelo identificador com balanço energético se houver avaliação (T-089).
	 * Não retorna 409 se não houver avaliação — apenas omite o energyBalance.
	 */
	@Operation(summary = "Buscar dieta por id",
			description = "Retorna uma versão específica da dieta pelo identificador com balanço energético (se houver avaliação). "
					+ "Versões anteriores são imutáveis (readOnly=true) e nunca sobrescritas (US-015). "
					+ "Se atleta não tem avaliação, energyBalance vem null (use /energy-balance para 409 acionável).")
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
		
		// Tenta calcular balanço energético se houver avaliação (não bloqueia se não houver)
		EnergyBalance balance = null;
		AssessmentApplicationService.EnergyProfile profile = assessmentService
				.latestEnergyProfile(diet.athleteId().value())
				.orElse(null);
		if (profile != null) {
			balance = EnergyBalanceCalculator.calculate(diet.macros().calories(), profile.getKcal());
		}
		
		return DietResponse.from(diet, weightKg, balance);
	}

	/** Versão mais recente da dieta de um atleta (US-007) com balanço energético (T-089). */
	@Operation(summary = "Versão mais recente da dieta",
			description = "Retorna a versão mais recente da dieta de um atleta com balanço energético (T-089). "
					+ "Atleta: só vê dieta APROVADA; coach vê a mais recente (rascunho/revisão/aprovada). "
					+ "Se versão mais alta for ARCHIVED, responde 404 (sem plano ativo — T-089). "
					+ "Se atleta não tem avaliação, energyBalance vem null e response 409 no endpoint dedicado.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Versão mais recente retornada"),
			@ApiResponse(responseCode = "404", description = "Nenhuma dieta ativa para o atleta (ou versão mais alta arquivada)"),
			@ApiResponse(responseCode = "409", description = "Atleta sem avaliação (apenas no endpoint /energy-balance)") })
	// T-010/US-038: atleta só consulta os próprios dados (claim athleteId do JWT).
	// T-058: coach só consulta atletas da própria carteira — o athleteId de outro
	// coach resulta em 404 (não vaza a existência do recurso).
	@PreAuthorize("@athleteGuard.canAccess(authentication, #athleteId)")
	@GetMapping
	public DietResponse latest(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Identificador do atleta") @RequestParam UUID athleteId) {
		coachAthleteGuard.requireCoachOwnsAthlete(jwt, athleteId);
		boolean athleteView = CoachAthleteAccessGuard.isAthlete(jwt);
		try {
			DietApplicationService.DietWithEnergyBalance result = service.latestWithEnergyBalance(athleteId, athleteView);
			double weightKg = getAthleteWeightKg(athleteId);
			return DietResponse.from(result.diet(), weightKg, result.balance());
		} catch (DietApplicationService.NoAssessmentException e) {
			// Atleta sem avaliação: retorna dieta sem balanço (energyBalance = null)
			// O endpoint /energy-balance retornará 409
			Diet diet = service.latestByAthlete(athleteId, athleteView);
			double weightKg = getAthleteWeightKg(athleteId);
			return DietResponse.from(diet, weightKg, null);
		}
	}

	/**
	 * Histórico de versões da dieta de um atleta com balanço energético (T-089).
	 */
	@Operation(summary = "Histórico de versões da dieta",
			description = "Retorna o histórico encadeado de versões da dieta de um atleta (v1→v2→v3) com balanço energético, "
					+ "evidência da regra de nunca sobrescrever (US-015): cada item aponta para previousVersionId. "
					+ "Atleta: só versões APPROVED/ARCHIVED não removidas. Coach: todas as versões.")
	@ApiResponse(responseCode = "200", description = "Histórico retornado com sucesso")
	// T-010/US-038: atleta só consulta os próprios dados (claim athleteId do JWT).
	// T-058: coach só consulta atletas da própria carteira.
	@PreAuthorize("@athleteGuard.canAccess(authentication, #athleteId)")
	@GetMapping("/history")
	public List<DietResponse> history(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Identificador do atleta") @RequestParam UUID athleteId) {
		coachAthleteGuard.requireCoachOwnsAthlete(jwt, athleteId);
		boolean athleteView = CoachAthleteAccessGuard.isAthlete(jwt);
		double weightKg = getAthleteWeightKg(athleteId);
		
		// Para histórico, calculamos o balanço para cada versão que tem avaliação
		// Nota: na prática, a avaliação é a mesma para todas as versões (a mais recente)
		AssessmentApplicationService.EnergyProfile profile = assessmentService
				.latestEnergyProfile(athleteId)
				.orElse(null);
		
		EnergyBalance balance = null;
		if (profile != null) {
			// O balanço será calculado por versão no stream abaixo
		}
		
		return service.historyByAthlete(athleteId, athleteView).stream()
				.map(diet -> {
					EnergyBalance dietBalance = null;
					if (profile != null) {
						dietBalance = EnergyBalanceCalculator.calculate(diet.macros().calories(), profile.getKcal());
					}
					return DietResponse.from(diet, weightKg, dietBalance);
				})
				.toList();
	}

	// ===== T-085: Meal Management Endpoints =====

	@Operation(summary = "Adicionar refeição à dieta",
			description = "Adiciona uma refeição a uma versão da dieta (T-085) com balanço energético atualizado (T-089). "
					+ "Apenas versões não read-only (DRAFT).")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Refeição adicionada"),
			@ApiResponse(responseCode = "404", description = "Dieta não encontrada ou de outro coach"),
			@ApiResponse(responseCode = "409", description = "Dieta somente leitura ou removida; ou atleta sem avaliação (energyBalance virá null)") })
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
		
		// Tenta calcular balanço energético se houver avaliação
		EnergyBalance balance = null;
		AssessmentApplicationService.EnergyProfile profile = assessmentService
				.latestEnergyProfile(diet.athleteId().value())
				.orElse(null);
		if (profile != null) {
			balance = EnergyBalanceCalculator.calculate(diet.macros().calories(), profile.getKcal());
		}
		
		return ResponseEntity.created(location).body(DietResponse.from(diet, weightKg, balance));
	}

	@Operation(summary = "Adicionar item a uma refeição",
			description = "Adiciona um item de alimento a uma refeição da dieta (T-085) com balanço energético atualizado (T-089). "
					+ "Macros são snapshot no momento da prescrição.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Item adicionado"),
			@ApiResponse(responseCode = "404", description = "Dieta ou refeição não encontrada"),
			@ApiResponse(responseCode = "409", description = "Dieta somente leitura ou removida; ou atleta sem avaliação (energyBalance virá null)") })
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
		
		// Tenta calcular balanço energético se houver avaliação
		EnergyBalance balance = null;
		AssessmentApplicationService.EnergyProfile profile = assessmentService
				.latestEnergyProfile(diet.athleteId().value())
				.orElse(null);
		if (profile != null) {
			balance = EnergyBalanceCalculator.calculate(diet.macros().calories(), profile.getKcal());
		}
		
		return DietResponse.from(diet, weightKg, balance);
	}

	@Operation(summary = "Remover item de uma refeição",
			description = "Remove um item de uma refeição da dieta (T-085) com balanço energético atualizado (T-089). "
					+ "Recalcula os totais automaticamente.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Item removido"),
			@ApiResponse(responseCode = "404", description = "Dieta, refeição ou item não encontrado"),
			@ApiResponse(responseCode = "409", description = "Dieta somente leitura ou removida; ou atleta sem avaliação (energyBalance virá null)") })
	@PreAuthorize("hasRole('COACH')")
	@DeleteMapping("/{id}/meals/{mealId}/items/{itemId}")
	public DietResponse removeMealItem(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Identificador da dieta") @PathVariable UUID id,
			@Parameter(description = "Identificador da refeição") @PathVariable UUID mealId,
			@Parameter(description = "Identificador do item") @PathVariable UUID itemId) {
		Diet diet = service.removeMealItem(UUID.fromString(jwt.getSubject()), id, mealId, itemId);
		double weightKg = getAthleteWeightKg(diet.athleteId().value());
		
		// Tenta calcular balanço energético se houver avaliação
		EnergyBalance balance = null;
		AssessmentApplicationService.EnergyProfile profile = assessmentService
				.latestEnergyProfile(diet.athleteId().value())
				.orElse(null);
		if (profile != null) {
			balance = EnergyBalanceCalculator.calculate(diet.macros().calories(), profile.getKcal());
		}
		
		return DietResponse.from(diet, weightKg, balance);
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
		
		// Tenta calcular balanço energético se houver avaliação
		EnergyBalance balance = null;
		AssessmentApplicationService.EnergyProfile profile = assessmentService
				.latestEnergyProfile(diet.athleteId().value())
				.orElse(null);
		if (profile != null) {
			balance = EnergyBalanceCalculator.calculate(diet.macros().calories(), profile.getKcal());
		}
		
		return DietResponse.from(diet, weightKg, balance);
	}

	// ===== T-090: Supplement Management Endpoints =====

	@Operation(summary = "Adicionar suplemento à dieta",
			description = "Adiciona um suplemento a uma versão da dieta (T-090) com balanço energético atualizado (T-089). "
					+ "Suplementos não calóricos (creatina, multivitamínico) têm countsTowardMacros=false. "
					+ "Suplementos calóricos (Whey) devem ser adicionados como MealItem com kind=SUPPLEMENT_CALORIC. "
					+ "Apenas versões não read-only (DRAFT).")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Suplemento adicionado"),
			@ApiResponse(responseCode = "404", description = "Dieta não encontrada ou de outro coach"),
			@ApiResponse(responseCode = "409", description = "Dieta somente leitura ou removida") })
	@PreAuthorize("hasRole('COACH')")
	@PostMapping("/{id}/supplements")
	public ResponseEntity<DietResponse> addSupplement(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Identificador da dieta") @PathVariable UUID id,
			@Valid @RequestBody AddSupplementRequest request) {
		Diet diet = service.addSupplement(UUID.fromString(jwt.getSubject()), id,
				new DietApplicationService.AddSupplementCommand(UUID.fromString(jwt.getSubject()), id,
						request.name(), request.dose(), request.timing(), request.countsTowardMacros()));
		URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
				.path("/api/v1/diets/{id}")
				.buildAndExpand(diet.id().value())
				.toUri();
		double weightKg = getAthleteWeightKg(diet.athleteId().value());
		
		EnergyBalance balance = null;
		AssessmentApplicationService.EnergyProfile profile = assessmentService
				.latestEnergyProfile(diet.athleteId().value())
				.orElse(null);
		if (profile != null) {
			balance = EnergyBalanceCalculator.calculate(diet.macros().calories(), profile.getKcal());
		}
		
		return ResponseEntity.created(location).body(DietResponse.from(diet, weightKg, balance));
	}

	@Operation(summary = "Remover suplemento da dieta",
			description = "Remove um suplemento da dieta pelo nome (T-090). Apenas versões não read-only (DRAFT).")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Suplemento removido"),
			@ApiResponse(responseCode = "404", description = "Dieta ou suplemento não encontrado"),
			@ApiResponse(responseCode = "409", description = "Dieta somente leitura ou removida") })
	@PreAuthorize("hasRole('COACH')")
	@DeleteMapping("/{id}/supplements/{supplementName}")
	public DietResponse removeSupplement(@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Identificador da dieta") @PathVariable UUID id,
			@Parameter(description = "Nome do suplemento") @PathVariable String supplementName) {
		Diet diet = service.removeSupplement(UUID.fromString(jwt.getSubject()), id, supplementName);
		double weightKg = getAthleteWeightKg(diet.athleteId().value());
		
		EnergyBalance balance = null;
		AssessmentApplicationService.EnergyProfile profile = assessmentService
				.latestEnergyProfile(diet.athleteId().value())
				.orElse(null);
		if (profile != null) {
			balance = EnergyBalanceCalculator.calculate(diet.macros().calories(), profile.getKcal());
		}
		
		return DietResponse.from(diet, weightKg, balance);
	}

	// ===== T-088: AI Plan Proposal Endpoints =====

	/**
	 * Proposta de plano alimentar gerado por IA (T-088).
	 * Recebe o contrato JSON v1, valida, persiste como DRAFT com requiresCoachApproval=true,
	 * registra auditoria AI_PLAN_PROPOSED. Nunca aprova automaticamente.
	 */
	@Operation(summary = "Propor plano alimentar gerado por IA",
			description = "Recebe o contrato JSON v1 do plano gerado por IA (docs/nutricao-plano-alimentar.md §4), " +
					"valida, persiste como nova versão em DRAFT com requiresCoachApproval=true, " +
					"registra auditoria AI_PLAN_PROPOSED. O coach deve revisar e aprovar manualmente. " +
					"Se baseDietVersionId for informado, cria nova versão encadeada; caso contrário, cria a primeira dieta do atleta.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Plano proposto e persistido como DRAFT"),
			@ApiResponse(responseCode = "400", description = "Contrato inválido ou validação falhou"),
			@ApiResponse(responseCode = "404", description = "Atleta não encontrado ou não pertence ao coach"),
			@ApiResponse(responseCode = "409", description = "Atleta já possui dieta (use baseDietVersionId) ou versão base inválida") })
	@PreAuthorize("hasRole('COACH')")
	@PostMapping("/ai-proposal")
	public ResponseEntity<AiPlanProposalResponse> proposeAiPlan(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody AiPlanProposalRequest request) {
		// Converte request para domain object
		AiPlanProposal proposal = convertRequestToProposal(request, UUID.fromString(jwt.getSubject()));

		// Propõe o plano (persiste)
		Diet diet = service.proposeAiPlan(
				new DietApplicationService.ProposeAiPlanCommand(
						UUID.fromString(jwt.getSubject()),
						request.athlete().athleteId(),
						proposal));

		URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
				.path("/api/v1/diets/{id}")
				.buildAndExpand(diet.id().value())
				.toUri();

		double weightKg = getAthleteWeightKg(diet.athleteId().value());
		UUID previousVersionId = diet.previousVersionId() != null ? diet.previousVersionId().value() : null;
		return ResponseEntity.created(location)
				.body(AiPlanProposalResponse.from(diet, weightKg, true, previousVersionId));
	}

	/**
	 * Validação dry-run de proposta de IA (T-088).
	 * Não persiste; apenas valida o contrato, calcula warnings, consistência e derivados,
	 * e retorna a proposta enriquecida para o coach revisar antes de confirmar.
	 */
	@Operation(summary = "Validar proposta de IA (dry-run)",
			description = "Valida o contrato JSON v1 sem persistir. Calcula warnings nutricionais, " +
					"consistência 4/4/9, macros derivados (g/kg, %kcal) e balanço energético " +
					"com base na avaliação mais recente do atleta. Útil para preview antes de confirmar.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Proposta validada e enriquecida"),
			@ApiResponse(responseCode = "400", description = "Contrato inválido"),
			@ApiResponse(responseCode = "404", description = "Atleta não encontrado ou sem avaliação para basear o balanço"),
			@ApiResponse(responseCode = "409", description = "Atleta sem avaliação para calcular balanço") })
	@PreAuthorize("hasRole('COACH')")
	@PostMapping("/ai-proposal/validate")
	public AiPlanProposalResponse validateAiPlan(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody AiPlanProposalRequest request) {
		CoachId coachId = coachService.requireCoachIdByUserId(UUID.fromString(jwt.getSubject()));
		athleteAccessService.requireOwnedByCoach(request.athlete().athleteId(), coachId.value());

		// Busca perfil energético do atleta (avaliação mais recente)
		var energyProfile = assessmentService.latestEnergyProfile(request.athlete().athleteId())
				.orElseThrow(() -> new IllegalStateException("Athlete has no assessment; register one to validate energy balance"));

		double weightKg = request.athlete().weightKg();
		double tmbKcal = energyProfile.tmbKcal().doubleValue();
		double getKcal = energyProfile.getKcal().doubleValue();

		// Converte request para domain object
		AiPlanProposal proposal = convertRequestToProposal(request, UUID.fromString(jwt.getSubject()));

		// Valida (dry-run)
		AiPlanProposal validated = service.validateAiPlan(proposal, weightKg, tmbKcal, getKcal);

		return AiPlanProposalResponse.fromValidation(validated, weightKg);
	}

	/**
	 * Converte o request DTO para o objeto de domínio AiPlanProposal.
	 */
	private AiPlanProposal convertRequestToProposal(AiPlanProposalRequest request, UUID coachUserId) {
		CoachId coachId = coachService.requireCoachIdByUserId(coachUserId);
		AthleteId athleteId = AthleteId.of(request.athlete().athleteId());

		// Converte refeições
		List<AiPlanProposal.AiPlanMeal> meals = request.meals().stream().map(m -> {
			List<AiPlanProposal.AiPlanMealItem> items = m.items().stream().map(i ->
					new AiPlanProposal.AiPlanMealItem(
							i.foodId(),
							i.name(),
							i.source(),
							i.kind(),
							i.state(),
							i.quantityG(),
							i.householdMeasure(),
							i.preparation(),
							Macronutrients.of(i.macros().calories(), i.macros().protein(),
									i.macros().carbohydrates(), i.macros().fat())
					)).toList();

			AiPlanProposal.MealTarget target = m.target() != null ?
					new AiPlanProposal.MealTarget(
							m.target().calories(), m.target().protein(),
							m.target().carbohydrates(), m.target().fat()) : null;

			Macronutrients totals = m.totals() != null ?
					Macronutrients.of(m.totals().calories(), m.totals().protein(),
							m.totals().carbohydrates(), m.totals().fat()) : null;

			return new AiPlanProposal.AiPlanMeal(
					m.mealId(), m.name(), m.order(), m.time(), m.notes(), target, items, totals);
		}).toList();

		// Converte suplementos
		List<AiPlanProposal.AiPlanSupplement> supplements = request.supplements() != null ?
				request.supplements().stream().map(s ->
						new AiPlanProposal.AiPlanSupplement(
								s.name(), s.dose(), s.timing(), s.countsTowardMacros()
						)).toList() : List.of();

		// Converte warnings
		List<DietWarning> warnings = request.warnings().stream().map(w ->
				new DietWarning(
						DietWarning.WarningCode.valueOf(w.code()),
						DietWarning.Severity.valueOf(w.severity()),
						w.message()
				)
		).toList();

		// Derivados
		AiPlanProposal.AiPlanDerived derived = new AiPlanProposal.AiPlanDerived(
				request.derived().tmbKcal(),
				request.derived().getKcal(),
				request.derived().balanceKcal(),
				request.derived().balancePercent(),
				request.derived().classification(),
				request.derived().proteinGPerKg(),
				request.derived().carbohydratesGPerKg(),
				request.derived().fatGPerKg(),
				request.derived().proteinPctKcal(),
				request.derived().carbohydratesPctKcal(),
				request.derived().fatPctKcal()
		);

		// Consistência
		AiPlanProposal.AiPlanConsistency consistency = new AiPlanProposal.AiPlanConsistency(
				request.consistency().calculatedCalories(),
				request.consistency().declaredCalories(),
				request.consistency().deltaKcal(),
				request.consistency().deltaPercent(),
				request.consistency().isConsistent()
		);

		// Base version
		DietId baseDietVersionId = request.meta().baseDietVersionId() != null ?
				DietId.of(request.meta().baseDietVersionId()) : null;

		return AiPlanProposal.builder()
				.id(UUID.randomUUID())
				.coachId(coachId)
				.athleteId(athleteId)
				.objective(request.strategy().objective())
				.targets(Macronutrients.of(
						request.targets().calories(),
						request.targets().protein(),
						request.targets().carbohydrates(),
						request.targets().fat()))
				.planMode(request.planMode())
				.meals(meals)
				.supplements(supplements)
				.warnings(warnings)
				.derived(derived)
				.consistency(consistency)
				.generatedAt(Instant.parse(request.meta().generatedAt()))
				.source(request.meta().source())
				.baseDietVersionId(baseDietVersionId)
				.disclaimer(request.disclaimer())
				.strategyNotes(request.strategy().notes())
				.phase(request.strategy().phase())
				.trainingDay(request.strategy().trainingDay() != null && request.strategy().trainingDay())
				.build();
	}

}
