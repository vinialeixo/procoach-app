package com.fitness.procoach.security.interfaces;

import com.fitness.procoach.security.application.AthleteInviteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Convite do atleta (primeira credencial): o coach gera o link, o atleta
 * consulta e aceita sem estar autenticado (o token do convite é a credencial).
 */
@Tag(name = "Athlete invite",
		description = "Primeira credencial do atleta: o coach gera um convite de uso único (7 dias) e o atleta "
				+ "define a própria senha pelo link.")
@RestController
@RequestMapping("/api/v1/auth/invites")
public class AthleteInviteController {

	private final AthleteInviteService inviteService;

	public AthleteInviteController(AthleteInviteService inviteService) {
		this.inviteService = inviteService;
	}

	@Operation(summary = "Gerar convite para um atleta",
			description = "Cria um convite de uso único para um atleta da carteira do coach. Convites pendentes "
					+ "anteriores do mesmo atleta são revogados. O token só é devolvido nesta resposta.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Convite criado"),
			@ApiResponse(responseCode = "404", description = "Atleta não encontrado ou de outro coach"),
			@ApiResponse(responseCode = "409", description = "Atleta já tem acesso, ou e-mail já cadastrado") })
	@PreAuthorize("hasRole('COACH')")
	@PostMapping
	public ResponseEntity<InviteResponse> create(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CreateInviteRequest request) {
		AthleteInviteService.CreatedInvite created = inviteService.create(UUID.fromString(jwt.getSubject()),
				request.athleteId(), request.email());
		return ResponseEntity.status(HttpStatus.CREATED).body(new InviteResponse(created.athleteId(),
				created.email(), created.token(), "/convite/" + created.token(), created.expiresAt()));
	}

	@Operation(summary = "Consultar convite",
			description = "Público: devolve nome do atleta e e-mail do convite para a tela de aceite. "
					+ "404 genérico para convite inexistente, expirado, revogado ou já usado.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Convite válido"),
			@ApiResponse(responseCode = "404", description = "Convite inválido ou expirado") })
	@GetMapping("/{token}")
	public InvitePreviewResponse preview(@PathVariable String token) {
		AthleteInviteService.InvitePreview preview = inviteService.preview(token);
		return new InvitePreviewResponse(preview.athleteName(), preview.email(), preview.expiresAt());
	}

	@Operation(summary = "Aceitar convite",
			description = "Público: cria a credencial do atleta com o e-mail do convite e a senha escolhida, "
					+ "consome o convite e devolve a sessão autenticada.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Credencial criada; sessão aberta"),
			@ApiResponse(responseCode = "400", description = "Senha inválida"),
			@ApiResponse(responseCode = "404", description = "Convite inválido ou expirado"),
			@ApiResponse(responseCode = "409", description = "Atleta já tem acesso, ou e-mail já cadastrado") })
	@PostMapping("/{token}/accept")
	public TokenResponse accept(@PathVariable String token, @Valid @RequestBody AcceptInviteRequest request) {
		return TokenResponse.from(inviteService.accept(token, request.password()));
	}
}
