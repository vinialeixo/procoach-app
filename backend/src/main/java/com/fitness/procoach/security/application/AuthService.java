package com.fitness.procoach.security.application;

import java.util.Set;
import java.util.UUID;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.fitness.procoach.audit.application.AuditData;
import com.fitness.procoach.audit.application.AuditService;
import com.fitness.procoach.coach.application.CoachApplicationService;
import com.fitness.procoach.security.domain.Permission;
import com.fitness.procoach.security.domain.Role;
import com.fitness.procoach.security.domain.RoleRepository;
import com.fitness.procoach.security.domain.User;
import com.fitness.procoach.security.domain.UserRepository;

/**
 * Fachada de autenticação do ProCoach (T-008): login em duas etapas
 * (senha -> TOTP quando o MFA está ativo), emissão/renovação de JWT e
 * ativação do MFA. Todo o acesso a domínio passa pelas portas
 * {@link UserRepository}/{@link RoleRepository}.
 */
@Service
public class AuthService {

	public static final String COACH_ROLE = "COACH";
	public static final String ATHLETE_ROLE = "ATHLETE";

	private static final String TOTP_ISSUER = "ProCoach";

	/**
	 * Máximo de tentativas para gravações na conta do usuário que perderam uma
	 * corrida de concorrência (guarda de versão do {@code @Version} do JPA).
	 */
	private static final int OPTIMISTIC_LOCK_MAX_ATTEMPTS = 3;

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenService tokenService;
	private final TotpService totpService;
	private final CoachApplicationService coachService;
	private final TransactionTemplate transactionTemplate;

	/**
	 * MFA/TOTP desligado por padrão (ver backlog T-059). Com a flag desligada o
	 * login não exige o segundo fator, mesmo que a conta tenha o MFA ativado.
	 */
	private final boolean mfaEnabled;

	private final AuditService auditService;

	public AuthService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder,
			JwtTokenService tokenService, TotpService totpService, CoachApplicationService coachService,
			PlatformTransactionManager transactionManager,
			@Value("${procoach.security.mfa.enabled:false}") boolean mfaEnabled,
			AuditService auditService) {
		this.auditService = auditService;
		this.mfaEnabled = mfaEnabled;
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
		this.totpService = totpService;
		this.coachService = coachService;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	/** Resultado do login: ou o usuário já está autenticado, ou falta o segundo fator. */
	public record LoginResult(boolean mfaRequired, String challengeId, AuthTokens tokens) {

		static LoginResult mfaRequired(String challengeId) {
			return new LoginResult(true, challengeId, null);
		}

		static LoginResult authenticated(AuthTokens tokens) {
			return new LoginResult(false, null, tokens);
		}
	}

	/** Resultado do cadastro de coach: usuário criado + sessão já autenticada. */
	public record CoachRegistration(UUID userId, AuthTokens tokens) {
	}

	@Transactional
	public CoachRegistration registerCoach(String name, String email, String rawPassword) {
		if (userRepository.findByEmail(email).isPresent()) {
			throw new EmailAlreadyRegisteredException(email);
		}
		Role coach = findOrCreateRole(COACH_ROLE, Set.of(Permission.MANAGE_ATHLETES, Permission.MANAGE_PROTOCOLS));
		User user = User.create(email, email, passwordEncoder.encode(rawPassword), Set.of(coach.getId()));
		User saved = userRepository.save(user);
		// T-058: o cadastro do coach também cria o agregado Coach vinculado ao
		// User.id (subject do JWT) — âncora da resolução server-side do coachId.
		coachService.registerCoach(saved.getId(), name);
		auditService.record("User", saved.getId(), null, "COACH_REGISTERED", saved.getId(), AuditData.of());
		return new CoachRegistration(saved.getId(), issueTokens(user));
	}

	/**
	 * Emite a credencial de um atleta, vinculada ao seu agregado (referência por
	 * UUID). O vínculo vira o claim {@code athleteId} do JWT, âncora do
	 * isolamento de dados da T-010.
	 */
	@Transactional
	public CoachRegistration registerAthleteCredential(String email, String rawPassword, UUID athleteId) {
		if (userRepository.findByEmail(email).isPresent()) {
			throw new EmailAlreadyRegisteredException(email);
		}
		Role athlete = findOrCreateRole(ATHLETE_ROLE, Set.of(Permission.VIEW_OWN_DATA));
		User user = User.create(email, email, passwordEncoder.encode(rawPassword), Set.of(athlete.getId()));
		user.linkAthlete(athleteId);
		User saved = userRepository.save(user);
		auditService.record("User", saved.getId(), athleteId, "ATHLETE_CREDENTIAL_CREATED", saved.getId(),
				AuditData.of());
		return new CoachRegistration(saved.getId(), issueTokens(user));
	}

	@Transactional
	public LoginResult login(String email, String rawPassword) {
		User user = userRepository.findByEmail(email)
				.filter(User::isEnabled)
				.orElseThrow(InvalidCredentialsException::new);
		if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
			throw new InvalidCredentialsException();
		}
		if (mfaEnabled && user.isMfaEnabled()) {
			return LoginResult.mfaRequired(tokenService.issueMfaChallenge(user));
		}
		recordLogin(user);
		return LoginResult.authenticated(issueTokens(user));
	}

	/** Segunda etapa do login: valida o desafio de MFA e o código TOTP. */
	@Transactional
	public AuthTokens verifyMfaLogin(String challengeId, String code) {
		Jwt challenge = tokenService.parse(challengeId);
		tokenService.requireType(challenge, JwtTokenService.TYPE_MFA_CHALLENGE);
		User user = loadEnabledUser(challenge.getSubject());
		if (!user.isMfaEnabled() || !totpService.verify(user.getMfaSecret(), code)) {
			throw new InvalidMfaCodeException();
		}
		recordLogin(user);
		return issueTokens(user);
	}

	/**
	 * Primeiro passo da ativação do MFA (usuário autenticado): gera o segredo TOTP.
	 * Cada tentativa roda em uma transação própria com o usuário recarregado do
	 * banco; se uma requisição concorrente gravar a mesma conta entre a leitura e o
	 * commit, a guarda de versão do {@code @Version} falha e a tentativa é repetida
	 * com estado fresco (no máximo {@value #OPTIMISTIC_LOCK_MAX_ATTEMPTS} vezes).
	 */
	public MfaSetupResult startMfaSetup(UUID userId) {
		return withOptimisticLockRetry(status -> doStartMfaSetup(userId));
	}

	private MfaSetupResult doStartMfaSetup(UUID userId) {
		User user = loadEnabledUser(userId.toString());
		if (user.isMfaEnabled()) {
			throw new MfaSetupException("MFA já está ativo para este usuário");
		}
		String secret = totpService.generateSecret();
		user.startMfaSetup(secret);
		userRepository.save(user);
		return new MfaSetupResult(secret, totpService.otpauthUrl(TOTP_ISSUER, user.getEmail(), secret));
	}

	/** Confirma a ativação do MFA com o primeiro código gerado pelo autenticador. */
	public void confirmMfaSetup(UUID userId, String code) {
		withOptimisticLockRetry(status -> {
			doConfirmMfaSetup(userId, code);
			return null;
		});
	}

	private void doConfirmMfaSetup(UUID userId, String code) {
		User user = loadEnabledUser(userId.toString());
		if (user.isMfaEnabled()) {
			throw new MfaSetupException("MFA já está ativo para este usuário");
		}
		if (user.getMfaSecret() == null || !totpService.verify(user.getMfaSecret(), code)) {
			throw new InvalidMfaCodeException();
		}
		user.confirmMfaSetup();
		userRepository.save(user);
	}

	@Transactional(readOnly = true)
	public AuthTokens refresh(String refreshToken) {
		Jwt jwt = tokenService.parse(refreshToken);
		tokenService.requireType(jwt, JwtTokenService.TYPE_REFRESH);
		return issueTokens(loadEnabledUser(jwt.getSubject()));
	}

	/** Login concluído (senha e, se ativo, segundo fator). O atleta aparece no histórico dele. */
	private void recordLogin(User user) {
		auditService.record("User", user.getId(), user.getAthleteId(), "USER_LOGGED_IN", user.getId(),
				AuditData.of("role", primaryRole(user)));
	}

	private AuthTokens issueTokens(User user) {
		String role = primaryRole(user);
		return new AuthTokens(tokenService.issueAccessToken(user, role), tokenService.issueRefreshToken(user),
				"Bearer", tokenService.accessTokenExpiresInSeconds(), role, user.getAthleteId());
	}

	/** Papel principal do usuário: COACH tem precedência sobre ATHLETE. */
	private String primaryRole(User user) {
		Set<String> names = new java.util.TreeSet<>();
		for (UUID roleId : user.getRoleIds()) {
			roleRepository.findById(roleId).filter(Role::isActive).ifPresent(role -> names.add(role.getName()));
		}
		if (names.contains(COACH_ROLE)) {
			return COACH_ROLE;
		}
		if (names.contains(ATHLETE_ROLE)) {
			return ATHLETE_ROLE;
		}
		return names.stream().findFirst().orElseThrow(InvalidCredentialsException::new);
	}

	private User loadEnabledUser(String subject) {
		User user = userRepository.findById(UUID.fromString(subject))
				.orElseThrow(() -> new InvalidTokenException("Usuário do token não existe"));
		if (!user.isEnabled()) {
			throw new InvalidTokenException("Usuário desabilitado");
		}
		return user;
	}

	/**
	 * Executa uma tentativa transacional de gravação na conta e a repete caso outra
	 * transação tenha commitado concorrentemente (a guarda de versão do
	 * {@code @Version} do JPA falhou com 0 linhas afetadas). A repetição é segura
	 * porque cada tentativa recarrega o agregado do banco e reaplica a mutação de
	 * domínio sobre o estado mais novo. Exceções de negócio propagam imediatamente
	 * (a transação da tentativa é revertida); só o conflito de versão é retentado.
	 */
	private <T> T withOptimisticLockRetry(TransactionCallback<T> attempt) {
		for (int attemptNumber = 1;; attemptNumber++) {
			try {
				return transactionTemplate.execute(attempt);
			} catch (ObjectOptimisticLockingFailureException ex) {
				if (attemptNumber >= OPTIMISTIC_LOCK_MAX_ATTEMPTS) {
					throw ex;
				}
			}
		}
	}

	private Role findOrCreateRole(String name, Set<Permission> permissions) {
		return roleRepository.findByName(name).orElseGet(() -> roleRepository.save(Role.create(name, permissions)));
	}
}
