package com.fitness.procoach.athlete.application;

import com.fitness.procoach.audit.application.AuditData;
import com.fitness.procoach.audit.application.AuditService;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service do módulo athlete (T-018). Orquestra o domínio e a porta
 * de persistência. Framework-agnostic: registrado como bean pela configuração
 * da camada de infraestrutura.
 */
public class AthleteApplicationService {

	private final AthleteRepository repository;
	private final CoachApplicationService coachService;
	private final AuditService auditService;
	private final AthleteAccessInfoProvider accessInfo;

	public AthleteApplicationService(AthleteRepository repository, CoachApplicationService coachService,
			AuditService auditService, AthleteAccessInfoProvider accessInfo) {
		this.auditService = auditService;
		this.repository = repository;
		this.coachService = coachService;
		this.accessInfo = accessInfo;
	}

	/**
	 * Cadastra um novo atleta (US-001). O {@link AthleteId} único e imutável é
	 * gerado aqui (US-002); o status inicial é {@code ACTIVE}. O coachId NUNCA
	 * vem do request (T-058): é resolvido server-side a partir do usuário
	 * autenticado (subject do JWT) via fachada do módulo coach.
	 */
	@Transactional
	public Athlete register(RegisterAthleteCommand command) {
		UUID coachId = coachService.requireCoachIdByUserId(command.userId()).value();
		// T-078: e-mail opcional; se já pertencer a um User, 409 acionável em vez
		// de deixar o conflito aparecer só na hora do convite.
		if (command.email() != null && !command.email().isBlank()
				&& accessInfo.emailBelongsToUser(command.email().trim())) {
			throw new EmailAlreadyRegisteredAsUserException(command.email().trim());
		}
		Athlete athlete = Athlete.register(
				AthleteId.generate(),
				coachId,
				new Name(command.name()),
				command.sex(),
				command.birthDate(),
				new Height(command.height()),
				new Weight(command.weight()),
				command.goal());
		Athlete saved = repository.save(athlete);
		auditService.record("Athlete", saved.id().value(), saved.id().value(), "ATHLETE_REGISTERED",
				command.userId(), AuditData.of("goal", saved.goal().name()));
		return saved;
	}

	/**
	 * Remove um atleta da carteira do coach autenticado (T-058). Exclusão é
	 * sempre soft-delete ({@link Athlete#softDelete()}) — o registro nunca é
	 * removido fisicamente. Lança {@link AthleteNotFoundException} (404) se o
	 * atleta não existe, foi removido ou pertence a outro coach.
	 */
	@Transactional
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
		auditService.record("Athlete", athlete.id().value(), athlete.id().value(), "ATHLETE_DELETED",
				command.userId(), AuditData.of());
	}

	/** Atletas da carteira do coach autenticado, com filtro opcional por status. */
	public List<Athlete> list(UUID userId, AthleteStatus status) {
		UUID coachId = coachService.requireCoachIdByUserId(userId).value();
		return repository.findActiveByCoachId(coachId).stream()
				.filter(a -> status == null || a.status() == status)
				.toList();
	}

	/** Histórico de auditoria do atleta (mais recente primeiro); só para o coach dono. */
	@Transactional(readOnly = true)
	public List<AuditService.AuditEntry> history(UUID userId, UUID athleteId) {
		UUID coachId = coachService.requireCoachIdByUserId(userId).value();
		requireOwned(athleteId, coachId);
		return auditService.entriesByAthlete(athleteId);
	}

	/** Ficha de um atleta da carteira; 404 se inexistente, removido ou de outro coach. */
	public Athlete get(UUID userId, UUID athleteId) {
		UUID coachId = coachService.requireCoachIdByUserId(userId).value();
		return requireOwned(athleteId, coachId);
	}

	/** Troca o status do atleta (US-005); mesma regra de posse do {@link #remove}. */
	@Transactional
	public Athlete changeStatus(ChangeStatusCommand command) {
		UUID coachId = coachService.requireCoachIdByUserId(command.userId()).value();
		Athlete athlete = requireOwned(command.athleteId(), coachId);
		AthleteStatus previous = athlete.status();
		athlete.changeStatus(command.status());
		Athlete saved = repository.save(athlete);
		auditService.record("Athlete", saved.id().value(), saved.id().value(), "ATHLETE_STATUS_CHANGED",
				command.userId(), AuditData.of("from", previous.name(), "to", saved.status().name(), "reason",
						command.reason()));
		return saved;
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
			BigDecimal height, BigDecimal weight, Goal goal, String email) {
	}

	public record RemoveAthleteCommand(UUID userId, UUID athleteId) {
	}
}
