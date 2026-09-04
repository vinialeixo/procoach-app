package com.fitness.procoach.security.interfaces;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fitness.procoach.security.application.AuthService;
import com.fitness.procoach.security.application.MfaSetupResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Autenticação do ProCoach (T-008/US-20): login em duas etapas
 * (e-mail/senha -> código TOTP quando o MFA está ativo), cadastro de coach,
 * ativação de MFA e renovação de token.
 */
@Tag(name = "Auth",
		description = "Autenticação com JWT + MFA (TOTP). Login em duas etapas: /login retorna o par de tokens "
				+ "ou um desafio de MFA (mfaRequired + challengeId) a ser resolvido em /mfa/verify.")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@Operation(summary = "Cadastrar coach",
			description = "Cria a conta do coach (ROLE_COACH) e já abre a sessão (auto-login): retorna o id do "
					+ "usuário e o par de tokens. Em seguida o front deve ativar o MFA (POST /mfa/setup).")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Conta criada com sucesso"),
			@ApiResponse(responseCode = "400", description = "Campos inválidos ou ausentes"),
			@ApiResponse(responseCode = "409", description = "E-mail já cadastrado") })
	@PostMapping("/register")
	public ResponseEntity<RegisterCoachResponse> register(@Valid @RequestBody RegisterCoachRequest request) {
		AuthService.CoachRegistration registration = authService.registerCoach(request.name(), request.email(),
				request.password());
		return ResponseEntity.status(HttpStatus.CREATED).body(RegisterCoachResponse.from(registration));
	}

	@Operation(summary = "Login (etapa 1)",
			description = "Valida e-mail/senha. Se o MFA estiver ativo, retorna mfaRequired=true + challengeId "
					+ "(chamar /mfa/verify com o código do autenticador); senão, retorna o par de tokens.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Autenticado ou desafio de MFA emitido"),
			@ApiResponse(responseCode = "401", description = "Credenciais inválidas") })
	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		return LoginResponse.from(authService.login(request.email(), request.password()));
	}

	@Operation(summary = "Iniciar ativação do MFA",
			description = "Gera o segredo TOTP e a otpauth:// URL (QR Code) para cadastro no aplicativo "
					+ "autenticador. Exige Bearer token válido; o MFA só é ativado em /mfa/setup/confirm.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Segredo gerado"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "409", description = "MFA já está ativo") })
	@PostMapping("/mfa/setup")
	public MfaSetupResponse startMfaSetup(@AuthenticationPrincipal Jwt jwt) {
		MfaSetupResult result = authService.startMfaSetup(UUID.fromString(jwt.getSubject()));
		return MfaSetupResponse.from(result);
	}

	@Operation(summary = "Confirmar ativação do MFA",
			description = "Valida o primeiro código de 6 dígitos do autenticador e ativa o MFA na conta. "
					+ "Exige Bearer token válido.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "MFA ativado"),
			@ApiResponse(responseCode = "400", description = "Código inválido"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "409", description = "MFA já está ativo") })
	@PostMapping("/mfa/setup/confirm")
	public Map<String, Boolean> confirmMfaSetup(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody MfaConfirmRequest request) {
		authService.confirmMfaSetup(UUID.fromString(jwt.getSubject()), request.code());
		return Map.of("confirmed", Boolean.TRUE);
	}

	@Operation(summary = "Verificar código MFA (etapa 2 do login)",
			description = "Valida o challengeId (emitido pelo /login) + código TOTP de 6 dígitos e retorna o par "
					+ "de tokens da sessão.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Autenticado"),
			@ApiResponse(responseCode = "400", description = "Código inválido"),
			@ApiResponse(responseCode = "401", description = "Desafio inválido ou expirado") })
	@PostMapping("/mfa/verify")
	public TokenResponse verifyMfa(@Valid @RequestBody MfaVerifyRequest request) {
		return TokenResponse.from(authService.verifyMfaLogin(request.challengeId(), request.code()));
	}

	@Operation(summary = "Renovar token",
			description = "Troca um refresh token válido por um novo par access + refresh token.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Tokens renovados"),
			@ApiResponse(responseCode = "401", description = "Refresh token inválido ou expirado") })
	@PostMapping("/refresh")
	public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
		return TokenResponse.from(authService.refresh(request.refreshToken()));
	}
}
