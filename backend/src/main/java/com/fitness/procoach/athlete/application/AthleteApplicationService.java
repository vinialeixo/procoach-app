package com.fitness.procoach.athlete.application;

import com.fitness.procoach.athlete.domain.Athlete;
import com.fitness.procoach.athlete.domain.AthleteStatus;
import com.fitness.procoach.athlete.domain.AthleteId;
import com.fitness.procoach.athlete.domain.AthleteRepository;
import com.fitness.procoach.athlete.domain.Goal;
import com.fitness.procoach.athlete.domain.Height;
import com.fitness.procoach.athlete.domain.Name;
import com.fitness.procoach.athlete.domain.Sex;
import com.fitness.procoach.athlete.domain.Weight;
import com.fitness.procoach.coach.application.CoachApplicationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Application service do módulo athlete (T-018). Orquestra o domínio e a porta
 * de persistência. Framework-agnostic: registrado como bean pela configuração
 * da camada de infraestrutura.
 */
public class AthleteApplicationService {

	private final AthleteRepository repository;
	private final CoachApplicationService coachService;

	public AthleteApplicationService(AthleteRepository repository, CoachApplicationService coachService) {
		this.repository = repository;
		this.coachService = coachService;
	}

	/**
	 * Cadastra um novo atleta (US-001). O {@link AthleteId} único e imutável é
	 * gerado aqui (US-002); o status inicial é {@code ACTIVE}. O coachId NUNCA
	 * vem do request (T-058): é resolvido server-side a partir do usuário
	 * autenticado (subject do JWT) via fachada do módulo coach.
	 */
	public Athlete register(RegisterAthleteCommand command) {
		UUID coachId = coachService.requireCoachIdByUserId(command.userId()).value();
		Athlete athlete = Athlete.register(
				AthleteId.generate(),
				coachId,
				new Name(command.name()),
				command.sex(),
				command.birthDate(),
				new Height(command.height()),
				new Weight(command.weight()),
				command.goal());
		return repository.save(athlete);
	}

	/**
	 * Remove um atleta da carteira do coach autenticado (T-058). Exclusão é
	 * sempre soft-delete ({@link Athlete#softDelete()}) — o registro nunca é
	 * removido fisicamente. Lança {@link AthleteNotFoundException} (404) se o
	 * atleta não existe, foi removido ou pertence a outro coach.
	 */
	public void remove(RemoveAthleteCommand command) {
		UUID coachId = coachService.requireCoachIdByUserId(command.userId()).value();
		Athlete athlete = repository.findById(AthleteId.of(command.athleteId()))
				.filter(a -> !a.isDeleted())
				.orElseThrow(() -> new AthleteNotFoundException(command.athleteId()));
		if (!athlete.coachId().equals(coachId)) {
			throw new AthleteNotFoundException(command.athleteId());
		}
		athlete.softDelete();
		repository.save(athlete);
	}

	/** Atletas da carteira do coach autenticado, com filtro opcional por status. */
	public List<Athlete> list(UUID userId, AthleteStatus status) {
		UUID coachId = coachService.requireCoachIdByUserId(userId).value();
		return repository.findActiveByCoachId(coachId).stream()
				.filter(a -> status == null || a.status() == status)
				.toList();
	}

	/** Ficha de um atleta da carteira; 404 se inexistente, removido ou de outro coach. */
	public Athlete get(UUID userId, UUID athleteId) {
		UUID coachId = coachService.requireCoachIdByUserId(userId).value();
		return requireOwned(athleteId, coachId);
	}

	/** Troca o status do atleta (US-005); mesma regra de posse do {@link #remove}. */
	public Athlete changeStatus(ChangeStatusCommand command) {
		UUID coachId = coachService.requireCoachIdByUserId(command.userId()).value();
		Athlete athlete = requireOwned(command.athleteId(), coachId);
		athlete.changeStatus(command.status());
		return repository.save(athlete);
	}

	private Athlete requireOwned(UUID athleteId, UUID coachId) {
		Athlete athlete = repository.findById(AthleteId.of(athleteId))
				.filter(a -> !a.isDeleted())
				.orElseThrow(() -> new AthleteNotFoundException(athleteId));
		if (!athlete.coachId().equals(coachId)) {
			throw new AthleteNotFoundException(athleteId);
		}
		return athlete;
	}

	public record ChangeStatusCommand(UUID userId, UUID athleteId, AthleteStatus status, String reason) {
	}

	public record RegisterAthleteCommand(UUID userId, String name, Sex sex, LocalDate birthDate,
			BigDecimal height, BigDecimal weight, Goal goal) {
	}

	public record RemoveAthleteCommand(UUID userId, UUID athleteId) {
	}
}
