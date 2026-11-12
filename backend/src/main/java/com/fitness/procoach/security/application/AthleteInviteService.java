package com.fitness.procoach.security.application;

import com.fitness.procoach.audit.application.AuditData;
import com.fitness.procoach.audit.application.AuditService;
import com.fitness.procoach.athlete.application.AthleteAccessService;
import com.fitness.procoach.coach.application.CoachApplicationService;
import com.fitness.procoach.security.domain.AthleteInvite;
import com.fitness.procoach.security.domain.AthleteInviteRepository;
import com.fitness.procoach.security.domain.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Convite do atleta (primeira credencial): o coach gera um link de uso único e
 * o atleta define a própria senha. O envio por e-mail não faz parte deste
 * serviço — o link é devolvido ao coach, que o repassa ao atleta.
 */
@Service
public class AthleteInviteService {

	/** Validade do convite. */
	public static final Duration TTL = Duration.ofDays(7);

	private static final SecureRandom RANDOM = new SecureRandom();

	private final AthleteInviteRepository invites;
	private final UserRepository users;
	private final CoachApplicationService coachService;
	private final AthleteAccessService athleteAccessService;
	private final AuthService authService;
	private final AuditService auditService;

	public AthleteInviteService(AthleteInviteRepository invites, UserRepository users,
			CoachApplicationService coachService, AthleteAccessService athleteAccessService,
			AuthService authService, AuditService auditService) {
		this.auditService = auditService;
		this.invites = invites;
		this.users = users;
		this.coachService = coachService;
		this.athleteAccessService = athleteAccessService;
		this.authService = authService;
	}

	/** Convite criado; o token só existe aqui, nunca é recuperável depois. */
	public record CreatedInvite(UUID athleteId, String email, String token, Instant expiresAt) {
	}

	/** O que o atleta vê antes de definir a senha. */
	public record InvitePreview(String athleteName, String email, Instant expiresAt) {
	}

	/**
	 * Gera um convite para um atleta da carteira do coach autenticado. Convites
	 * pendentes anteriores do mesmo atleta são revogados. O coachId vem do
	 * usuário autenticado (T-058), nunca do request; atleta de outro coach resulta
	 * em 404.
	 */
	@Transactional
	public CreatedInvite create(UUID coachUserId, UUID athleteId, String email) {
		UUID coachId = coachService.requireCoachIdByUserId(coachUserId).value();
		athleteAccessService.requireOwnedByCoach(athleteId, coachId);
		String cleanEmail = email.trim();
		if (users.existsByAthleteId(athleteId)) {
			throw new AthleteAlreadyHasCredentialException();
		}
		if (users.findByEmail(cleanEmail).isPresent()) {
			throw new EmailAlreadyRegisteredException(cleanEmail);
		}
		Instant now = Instant.now();
		for (AthleteInvite previous : invites.findOpenByAthleteId(athleteId)) {
			previous.revoke(now);
			invites.save(previous);
		}
		String token = newToken();
		AthleteInvite invite = invites.save(
				AthleteInvite.create(athleteId, coachId, cleanEmail, hash(token), now, now.plus(TTL)));
		auditService.record("AthleteInvite", invite.getId(), athleteId, "ATHLETE_INVITED", coachUserId,
				AuditData.of("expiresAt", invite.getExpiresAt()));
		return new CreatedInvite(athleteId, cleanEmail, token, now.plus(TTL));
	}

	/** Dados do convite para a tela de aceite; 404 genérico se não for utilizável. */
	@Transactional(readOnly = true)
	public InvitePreview preview(String token) {
		AthleteInvite invite = invites.findByTokenHash(hash(token))
				.filter(i -> i.isUsable(Instant.now()))
				.orElseThrow(InvalidInviteException::new);
		String name = athleteAccessService.profileOf(invite.getAthleteId()).name();
		return new InvitePreview(name, invite.getEmail(), invite.getExpiresAt());
	}

	/** Status do convite para a listagem do coach (T-077) — sem expor token. */
	public static String statusOf(AthleteInvite invite, Instant now) {
		if (invite.getUsedAt() != null) {
			return "ACCEPTED";
		}
		if (invite.getRevokedAt() != null) {
			return "REVOKED";
		}
		if (!now.isBefore(invite.getExpiresAt())) {
			return "EXPIRED";
		}
		return "PENDING";
	}

	/** Resumo do convite para o coach; o token nunca aparece (T-077). */
	public record InviteSummary(UUID id, UUID athleteId, String email, Instant expiresAt, Instant createdAt,
			String status) {
	}

	/**
	 * Lista os convites do atleta (qualquer status). O coach só enxerga atletas
	 * da própria carteira (404 caso contrário — anti-IDOR).
	 */
	@Transactional(readOnly = true)
	public List<InviteSummary> listByAthlete(UUID coachUserId, UUID athleteId) {
		UUID coachId = coachService.requireCoachIdByUserId(coachUserId).value();
		athleteAccessService.requireOwnedByCoach(athleteId, coachId);
		Instant now = Instant.now();
		return invites.findAllByAthleteId(athleteId).stream()
				.map(i -> new InviteSummary(i.getId(), i.getAthleteId(), i.getEmail(), i.getExpiresAt(),
						i.getCreatedAt(), statusOf(i, now)))
				.toList();
	}

	/**
	 * Revoga um convite pendente do atleta. Só o coach dono pode (anti-IDOR,
	 * 404); convite inexistente, de outro atleta ou não pendente → 404/409.
	 * Auditoria append-only: {@code ATHLETE_INVITE_REVOKED}.
	 */
	@Transactional
	public InviteSummary revoke(UUID coachUserId, UUID athleteId, UUID inviteId) {
		UUID coachId = coachService.requireCoachIdByUserId(coachUserId).value();
		athleteAccessService.requireOwnedByCoach(athleteId, coachId);
		AthleteInvite invite = invites.findById(inviteId)
				.filter(i -> i.getAthleteId().equals(athleteId))
				.orElseThrow(InvalidInviteException::new);
		Instant now = Instant.now();
		if (invite.getUsedAt() != null) {
			throw new IllegalStateException("Convite já aceito não pode ser revogado");
		}
		if (invite.getRevokedAt() != null) {
			throw new IllegalStateException("Convite já revogado");
		}
		if (!now.isBefore(invite.getExpiresAt())) {
			throw new IllegalStateException("Convite expirado não pode ser revogado");
		}
		invite.revoke(now);
		invites.save(invite);
		auditService.record("AthleteInvite", invite.getId(), athleteId, "ATHLETE_INVITE_REVOKED", coachUserId,
				AuditData.of("email", invite.getEmail()));
		return new InviteSummary(invite.getId(), invite.getAthleteId(), invite.getEmail(), invite.getExpiresAt(),
				invite.getCreatedAt(), statusOf(invite, now));
	}

	/** Convite pendente (não usado, não revogado, dentro do prazo) do atleta, se houver. */
	@Transactional(readOnly = true)
	public Optional<AthleteInvite> findPendingByAthleteId(UUID athleteId) {
		Instant now = Instant.now();
		return invites.findOpenByAthleteId(athleteId).stream()
				.filter(i -> i.isUsable(now))
				.findFirst();
	}

	/**
	 * Aceita o convite: cria a credencial do atleta (e-mail do convite + senha
	 * escolhida), consome o convite e devolve a sessão já autenticada. A linha do
	 * convite fica travada durante o aceite, então dois aceites simultâneos do
	 * mesmo link não criam duas credenciais.
	 */
	@Transactional
	public AuthTokens accept(String token, String password) {
		Instant now = Instant.now();
		AthleteInvite invite = invites.findByTokenHashForUpdate(hash(token))
				.filter(i -> i.isUsable(now))
				.orElseThrow(InvalidInviteException::new);
		athleteAccessService.profileOf(invite.getAthleteId());
		if (users.existsByAthleteId(invite.getAthleteId())) {
			throw new AthleteAlreadyHasCredentialException();
		}
		AuthService.CoachRegistration registration = authService.registerAthleteCredential(invite.getEmail(),
				password, invite.getAthleteId());
		invite.markUsed(now);
		invites.save(invite);
		auditService.record("AthleteInvite", invite.getId(), invite.getAthleteId(), "ATHLETE_INVITE_ACCEPTED",
				registration.userId(), AuditData.of());
		return registration.tokens();
	}

	private static String newToken() {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	static String hash(String token) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 indisponível", e);
		}
	}
}
