package com.fitness.procoach.security.interfaces;

import com.fitness.procoach.security.application.AthleteInviteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestão dos convites do atleta pelo coach (T-077): listagem do histórico de
 * convites (sem token) e revogação de convite pendente. Os paths ficam sob
 * {@code /api/v1/athletes/...} como espera o front; o módulo é o security,
 * dono do agregado de convites.
 */
@Tag(name = "Athlete invite (coach)",
		description = "Listagem e revogação dos convites de acesso do atleta (T-077).")
@RestController
@RequestMapping("/api/v1/athletes/{athleteId}/invites")
public class AthleteInviteAdminController {

	private final AthleteInviteService inviteService;

	public AthleteInviteAdminController(AthleteInviteService inviteService) {
		this.inviteService = inviteService;
	}

	@Operation(summary = "Listar convites do atleta",
			description = "Histórico de convites do atleta com status (PENDING/ACCEPTED/REVOKED/EXPIRED). "
					+ "Nunca inclui o token. Só o coach dono do atleta (404 caso contrário).")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Lista de convites"),
			@ApiResponse(responseCode = "404", description = "Atleta não encontrado ou de outro coach") })
	@PreAuthorize("hasRole('COACH')")
	@GetMapping
	public List<InviteSummaryResponse> list(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID athleteId) {
		return inviteService.listByAthlete(UUID.fromString(jwt.getSubject()), athleteId).stream()
				.map(InviteSummaryResponse::from)
				.toList();
	}

	@Operation(summary = "Revogar convite pendente",
			description = "Revoga um convite ainda pendente do atleta e registra ATHLETE_INVITE_REVOKED "
					+ "no audit log. Convite aceito/revogado/expirado resulta em 409.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Convite revogado"),
			@ApiResponse(responseCode = "404", description = "Atleta/convite não encontrado ou de outro coach"),
			@ApiResponse(responseCode = "409", description = "Convite não está pendente") })
	@PreAuthorize("hasRole('COACH')")
	@PostMapping("/{inviteId}/revoke")
	public InviteSummaryResponse revoke(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID athleteId,
			@PathVariable UUID inviteId) {
		return InviteSummaryResponse.from(inviteService.revoke(UUID.fromString(jwt.getSubject()), athleteId, inviteId));
	}

	@com.fasterxml.jackson.annotation.JsonPropertyOrder({ "id", "athleteId", "email", "expiresAt", "createdAt",
			"status" })
	record InviteSummaryResponse(UUID id, UUID athleteId, String email, java.time.Instant expiresAt,
			java.time.Instant createdAt, String status) {

		static InviteSummaryResponse from(AthleteInviteService.InviteSummary summary) {
			return new InviteSummaryResponse(summary.id(), summary.athleteId(), summary.email(),
					summary.expiresAt(), summary.createdAt(), summary.status());
		}
	}
}
