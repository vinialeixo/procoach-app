package com.fitness.procoach.notification.interfaces;

import com.fitness.procoach.notification.application.NotificationService;
import com.fitness.procoach.notification.application.NotificationSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

	private final NotificationSettingsService settings;

	public NotificationController(NotificationService service, NotificationSettingsService settings) {
		this.service = service;
		this.settings = settings;
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

	@Operation(summary = "Preferências de aviso",
			description = "Canais externos (e-mail e push) que o atleta aceita. Sem registro, todos ligados; o aviso "
					+ "dentro do app sempre existe.")
	@PreAuthorize("hasRole('ATHLETE')")
	@GetMapping("/preferences")
	public NotificationPreferencesResponse preferences(@AuthenticationPrincipal Jwt jwt) {
		return NotificationPreferencesResponse.from(settings.getPreferences(athleteIdOf(jwt)));
	}

	@Operation(summary = "Atualizar preferências de aviso")
	@PreAuthorize("hasRole('ATHLETE')")
	@PutMapping("/preferences")
	public NotificationPreferencesResponse updatePreferences(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody NotificationPreferencesRequest request) {
		return NotificationPreferencesResponse.from(
				settings.updatePreferences(athleteIdOf(jwt), request.emailEnabled(), request.pushEnabled()));
	}

	@Operation(summary = "Registrar dispositivo para push",
			description = "Guarda o token do dispositivo do atleta. Registrar o mesmo token de novo atualiza o "
					+ "registro (não duplica).")
	@ApiResponse(responseCode = "204", description = "Dispositivo registrado")
	@PreAuthorize("hasRole('ATHLETE')")
	@PostMapping("/devices")
	public ResponseEntity<Void> registerDevice(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody RegisterDeviceRequest request) {
		settings.registerDevice(athleteIdOf(jwt), request.token(), request.platform());
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Remover dispositivo de push",
			description = "Remove o dispositivo do atleta (por exemplo, ao sair da conta no aparelho).")
	@ApiResponse(responseCode = "204", description = "Dispositivo removido (ou já inexistente)")
	@PreAuthorize("hasRole('ATHLETE')")
	@DeleteMapping("/devices")
	public ResponseEntity<Void> unregisterDevice(@AuthenticationPrincipal Jwt jwt, @RequestParam String token) {
		settings.unregisterDevice(athleteIdOf(jwt), token);
		return ResponseEntity.noContent().build();
	}

	private static UUID athleteIdOf(Jwt jwt) {
		return UUID.fromString(jwt.getClaimAsString("athleteId"));
	}
}
