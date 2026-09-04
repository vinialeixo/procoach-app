package com.fitness.procoach.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.fitness.procoach.ProcoachApplication;
import com.fitness.procoach.TestcontainersConfiguration;
import com.fitness.procoach.security.application.AuthService;
import com.fitness.procoach.security.application.AuthTokens;
import com.fitness.procoach.security.application.InvalidCredentialsException;
import com.fitness.procoach.security.application.InvalidMfaCodeException;
import com.fitness.procoach.security.application.InvalidTokenException;
import com.fitness.procoach.security.application.MfaSetupResult;
import com.fitness.procoach.security.application.RoleService;
import com.fitness.procoach.security.application.TotpService;
import com.fitness.procoach.security.application.UserService;
import com.fitness.procoach.security.domain.Permission;
import com.fitness.procoach.security.domain.Role;
import com.fitness.procoach.security.domain.RoleRepository;
import com.fitness.procoach.security.domain.User;

@SpringBootTest(classes = ProcoachApplication.class)
@Import(TestcontainersConfiguration.class)
class SecurityModuleTest {

	@Autowired
	private UserService userService;

	@Autowired
	private RoleService roleService;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private AuthService authService;

	@Autowired
	private TotpService totpService;

	/** Papel compartilhado entre testes: o nome é único, então é find-or-create. */
	private Role findOrCreateRole(String name, Set<Permission> permissions) {
		return roleRepository.findByName(name).orElseGet(() -> roleService.createRole(name, permissions));
	}

	private static String uniqueEmail(String prefix) {
		return prefix + "-" + UUID.randomUUID() + "@test.com";
	}

	@Test
	void createsRoleAndUserWithRoleReference() {
		Role coach = findOrCreateRole("COACH", Set.of(Permission.MANAGE_ATHLETES, Permission.MANAGE_PROTOCOLS));

		User user = userService.createUser("coach1", "coach1@example.com", "hash", Set.of(coach.getId()));

		assertThat(user.getId()).isNotNull();
		assertThat(user.getRoleIds()).containsExactly(coach.getId());
		assertThat(user.isEnabled()).isTrue();

		User loaded = userService.findById(user.getId()).orElseThrow();
		assertThat(loaded.getUsername()).isEqualTo("coach1");
		assertThat(loaded.getRoleIds()).containsExactly(coach.getId());
	}

	@Test
	void rejectsUserCreationWithUnknownRole() {
		assertThatThrownBy(() -> userService.createUser("x", "x@example.com", "hash", Set.of(UUID.randomUUID())))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void assignsRoleToExistingUser() {
		Role athlete = findOrCreateRole("ATHLETE", Set.of(Permission.VIEW_OWN_DATA));
		User user = userService.createUser("athlete1", "athlete1@example.com", "hash", Set.of());

		User updated = userService.assignRole(user.getId(), athlete.getId());

		assertThat(updated.getRoleIds()).containsExactly(athlete.getId());
	}

	// ---- T-008: autenticação JWT + MFA (TOTP) ----

	@Test
	void totpGeneratesSecretAndVerifiesCurrentCode() {
		String secret = totpService.generateSecret();

		assertThat(secret).isNotBlank();
		assertThat(totpService.verify(secret, totpService.currentCode(secret))).isTrue();
		assertThat(totpService.verify(secret, "000000")).isFalse();
		assertThat(totpService.verify(secret, "abcdef")).isFalse();
		assertThat(totpService.otpauthUrl("ProCoach", "coach@test.com", secret))
				.startsWith("otpauth://totp/ProCoach:coach@test.com?secret=" + secret);
	}

	@Test
	void registerCoachAuthenticatesImmediatelyAndLoginWorksWithoutMfa() {
		String email = uniqueEmail("coach");

		AuthService.CoachRegistration registration = authService.registerCoach("Coach Teste", email, "senha123!");
		assertThat(registration.userId()).isNotNull();
		assertThat(registration.tokens().role()).isEqualTo("COACH");
		assertThat(registration.tokens().accessToken()).isNotBlank();
		assertThat(registration.tokens().refreshToken()).isNotBlank();

		AuthService.LoginResult login = authService.login(email, "senha123!");
		assertThat(login.mfaRequired()).isFalse();
		assertThat(login.tokens().role()).isEqualTo("COACH");
		assertThat(login.tokens().athleteId()).isNull();
	}

	@Test
	void loginRejectsWrongPassword() {
		String email = uniqueEmail("coach");
		authService.registerCoach("Coach Teste", email, "senha123!");

		assertThatThrownBy(() -> authService.login(email, "errada"))
				.isInstanceOf(InvalidCredentialsException.class);
		assertThatThrownBy(() -> authService.login(uniqueEmail("ninguem"), "senha123!"))
				.isInstanceOf(InvalidCredentialsException.class);
	}

	@Test
	void mfaFullFlowSetupConfirmAndTwoStepLogin() {
		String email = uniqueEmail("mfa");
		UUID userId = authService.registerCoach("Coach MFA", email, "senha123!").userId();

		// Antes de confirmar o setup, o login ainda não exige MFA.
		MfaSetupResult setup = authService.startMfaSetup(userId);
		assertThat(setup.secret()).isNotBlank();
		assertThat(setup.otpauthUrl()).contains(setup.secret());
		assertThat(authService.login(email, "senha123!").mfaRequired()).isFalse();

		// Código errado não ativa o MFA.
		assertThatThrownBy(() -> authService.confirmMfaSetup(userId, "999999"))
				.isInstanceOf(InvalidMfaCodeException.class);

		authService.confirmMfaSetup(userId, totpService.currentCode(setup.secret()));

		// Com o MFA ativo, o login passa a exigir a segunda etapa.
		AuthService.LoginResult login = authService.login(email, "senha123!");
		assertThat(login.mfaRequired()).isTrue();
		assertThat(login.challengeId()).isNotBlank();
		assertThat(login.tokens()).isNull();

		assertThatThrownBy(() -> authService.verifyMfaLogin(login.challengeId(), "999999"))
				.isInstanceOf(InvalidMfaCodeException.class);

		AuthTokens tokens = authService.verifyMfaLogin(login.challengeId(), totpService.currentCode(setup.secret()));
		assertThat(tokens.accessToken()).isNotBlank();
		assertThat(tokens.role()).isEqualTo("COACH");
	}

	@Test
	void athleteCredentialCarriesAthleteId() {
		UUID athleteId = UUID.randomUUID();

		AuthService.CoachRegistration registration = authService.registerAthleteCredential(uniqueEmail("athlete"),
				"senha123!", athleteId);

		assertThat(registration.tokens().role()).isEqualTo("ATHLETE");
		assertThat(registration.tokens().athleteId()).isEqualTo(athleteId);
	}

	@Test
	void refreshRotatesTokensAndRejectsWrongTokenType() {
		String email = uniqueEmail("refresh");
		AuthTokens tokens = authService.registerCoach("Coach Refresh", email, "senha123!").tokens();

		AuthTokens renewed = authService.refresh(tokens.refreshToken());
		assertThat(renewed.accessToken()).isNotBlank();
		assertThat(renewed.refreshToken()).isNotBlank();
		assertThat(renewed.role()).isEqualTo("COACH");

		assertThatThrownBy(() -> authService.refresh(tokens.accessToken()))
				.isInstanceOf(InvalidTokenException.class);
		assertThatThrownBy(() -> authService.refresh("token-invalido"))
				.isInstanceOf(InvalidTokenException.class);
	}
}
