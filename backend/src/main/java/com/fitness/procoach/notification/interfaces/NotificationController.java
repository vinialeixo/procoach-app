package com.fitness.procoach.notification.interfaces;

import com.fitness.procoach.notification.application.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Avisos in-app do atleta: o coach edita o treino ativo ou aprova a dieta e o atleta vê o aviso
 * aqui. Só o próprio atleta lê e marca como lido (claim athleteId do JWT).
 */
@Tag(name = "Notification",
		description = "Avisos in-app para o atleta (treino ativo editado, dieta aprovada). Só o próprio atleta acessa.")
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

	private final NotificationService service;

	public NotificationController(NotificationService service) {
		this.service = service;
	}

	@Operation(summary = "Listar avisos do atleta",
			description = "Avisos do atleta, do mais recente para o mais antigo; com unreadOnly=true, só os não lidos.")
	@ApiResponse(responseCode = "200", description = "Avisos retornados")
	// US-038: só o próprio atleta (claim athleteId do JWT); o coach não lê os avisos do atleta.
	@PreAuthorize("hasRole('ATHLETE') and @athleteGuard.canAccess(authentication, #athleteId)")
	@GetMapping
	public List<NotificationResponse> list(
			@Parameter(description = "Identificador do atleta") @RequestParam UUID athleteId,
			@Parameter(description = "Só os não lidos") @RequestParam(defaultValue = "false") boolean unreadOnly) {
		return service.list(athleteId, unreadOnly).stream().map(NotificationResponse::from).toList();
	}

	@Operation(summary = "Marcar aviso como lido")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Aviso marcado como lido"),
			@ApiResponse(responseCode = "404", description = "Aviso inexistente ou de outro atleta") })
	@PreAuthorize("hasRole('ATHLETE')")
	@PostMapping("/{id}/read")
	public NotificationResponse markRead(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		return NotificationResponse.from(service.markRead(athleteIdOf(jwt), id));
	}

	@Operation(summary = "Marcar todos os avisos como lidos")
	@ApiResponse(responseCode = "200", description = "Quantidade de avisos marcados")
	@PreAuthorize("hasRole('ATHLETE')")
	@PostMapping("/read-all")
	public Map<String, Integer> markAllRead(@AuthenticationPrincipal Jwt jwt) {
		return Map.of("marked", service.markAllRead(athleteIdOf(jwt)));
	}

	private static UUID athleteIdOf(Jwt jwt) {
		return UUID.fromString(jwt.getClaimAsString("athleteId"));
	}
}
