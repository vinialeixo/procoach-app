package com.fitness.procoach.nutrition.application;

import com.fitness.procoach.athlete.application.AthleteAccessService;
import com.fitness.procoach.coach.application.CoachApplicationService;
import com.fitness.procoach.coach.domain.CoachId;
import com.fitness.procoach.nutrition.domain.AthleteId;
import com.fitness.procoach.nutrition.domain.Diet;
import com.fitness.procoach.nutrition.domain.DietId;
import com.fitness.procoach.nutrition.domain.DietObjective;
import com.fitness.procoach.nutrition.domain.DietRepository;
import com.fitness.procoach.nutrition.domain.DietStatus;
import com.fitness.procoach.nutrition.domain.Macronutrients;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service do módulo nutrition. Orquestra o domínio e a porta de
 * persistência. Framework-agnostic: registrado como bean pela configuração da
 * camada de infraestrutura.
 */
public class DietApplicationService {

	private final DietRepository repository;
	private final CoachApplicationService coachService;
	private final AthleteAccessService athleteAccessService;

	public DietApplicationService(DietRepository repository, CoachApplicationService coachService,
			AthleteAccessService athleteAccessService) {
		this.repository = repository;
		this.coachService = coachService;
		this.athleteAccessService = athleteAccessService;
	}

	/**
	 * Cria a primeira versão (v1) de uma dieta, em estado Rascunho (US-007).
	 * Objetivo e macronutrientes são obrigatórios. O coachId NUNCA vem do
	 * request (T-058): é resolvido server-side a partir do usuário autenticado,
	 * e o atleta-alvo precisa pertencer a esse coach (senão 404). O createdBy
	 * (auditoria) também nunca vem do request: é o próprio subject do JWT.
	 */
	public Diet createDiet(CreateDietCommand command) {
		CoachId coachId = coachService.requireCoachIdByUserId(command.userId());
		athleteAccessService.requireOwnedByCoach(command.athleteId(), coachId.value());
		// Uma cadeia de versões por atleta: a primeira dieta é v1; depois só nova versão.
		if (repository.findLatestByAthleteId(AthleteId.of(command.athleteId())).isPresent()) {
			throw new IllegalStateException(
					"Athlete already has a diet; create a new version of the existing one");
		}
		Diet diet = Diet.create(
				coachId,
				AthleteId.of(command.athleteId()),
				command.objective(),
				Macronutrients.of(command.calories(), command.protein(),
						command.carbohydrates(), command.fat()),
				command.userId());
		return repository.save(diet);
	}

	/**
	 * Retorna a versão mais recente da dieta de um atleta (US-007).
	 */
	public Diet latestByAthlete(UUID athleteId) {
		return repository.findLatestByAthleteId(AthleteId.of(athleteId))
				.orElseThrow(() -> new DietNotFoundException(athleteId));
	}

	/**
	 * Histórico de versões da dieta de um atleta.
	 */
	public List<Diet> historyByAthlete(UUID athleteId) {
		return repository.findAllVersionsByAthleteId(AthleteId.of(athleteId));
	}

	public Diet findById(DietId id) {
		return repository.findById(id)
				.orElseThrow(() -> new DietNotFoundException(id.value()));
	}

	/**
	 * Busca uma dieta pelo id garantindo que pertence a um atleta da carteira
	 * do coach autenticado (T-058). Dieta inexistente OU de atleta de outro
	 * coach resulta em 404 — não vaza a existência do recurso (anti-IDOR: o id
	 * do recurso nunca é suficiente para acessá-lo).
	 */
	public Diet findOwnedById(DietId id, UUID userId) {
		Diet diet = repository.findById(id)
				.orElseThrow(() -> new DietNotFoundException(id.value()));
		CoachId coachId = coachService.requireCoachIdByUserId(userId);
		athleteAccessService.requireOwnedByCoach(diet.athleteId().value(), coachId.value());
		return diet;
	}

	/**
	 * Cria a próxima versão a partir de {@code dietId} (US-015/US-016): a versão
	 * de origem é congelada (readOnly) e o motivo da alteração é obrigatório.
	 * Mesma regra de posse do {@link #findOwnedById}.
	 */
	@Transactional
	public Diet createNewVersion(NewVersionCommand command) {
		Diet current = findOwnedById(new DietId(command.dietId()), command.userId());
		Diet next = current.createNewVersion(
				command.objective(),
				Macronutrients.of(command.calories(), command.protein(), command.carbohydrates(), command.fat()),
				command.changeReason(),
				command.userId());
		repository.save(current);
		return repository.save(next);
	}

	/** Transiciona o status da dieta (US-017); transição inválida lança IllegalStateException. */
	public Diet changeStatus(UUID userId, UUID dietId, DietStatus status) {
		Diet diet = findOwnedById(new DietId(dietId), userId);
		diet.changeStatus(status);
		return repository.save(diet);
	}

	public record NewVersionCommand(UUID userId, UUID dietId, DietObjective objective,
			double calories, double protein, double carbohydrates, double fat, String changeReason) {
	}

	public record CreateDietCommand(UUID userId, UUID athleteId, DietObjective objective,
			double calories, double protein, double carbohydrates, double fat) {
	}
}
