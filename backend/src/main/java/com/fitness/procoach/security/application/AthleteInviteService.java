package com.fitness.procoach.security.application;

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

	public AthleteInviteService(AthleteInviteRepository invites, UserRepository users,
			CoachApplicationService coachService, AthleteAccessService athleteAccessService,
			AuthService authService) {
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
		invites.save(AthleteInvite.create(athleteId, coachId, cleanEmail, hash(token), now, now.plus(TTL)));
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
