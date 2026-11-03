package com.fitness.procoach.athlete.interfaces;

import com.fitness.procoach.athlete.application.AthleteApplicationService;
import com.fitness.procoach.athlete.domain.Athlete;
import com.fitness.procoach.athlete.domain.AthleteStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Controller REST do cadastro de atletas (T-018/US-001).
 */
@Tag(name = "Athlete", description = "Cadastro e ficha de atletas. O id do atleta é único e imutável (US-002).")
@RestController
@RequestMapping("/api/v1/athletes")
public class AthleteController {

	private final AthleteApplicationService service;

	public AthleteController(AthleteApplicationService service) {
		this.service = service;
	}

	/**
	 * Cadastra um novo atleta. Retorna {@code 201 Created} com o DTO do atleta
	 * no corpo e o header {@code Location} apontando para o recurso criado.
	 * Campos inválidos/ausentes resultam em {@code 400 Bad Request} (ver
	 * {@link AthleteExceptionHandler}).
	 */
	@Operation(summary = "Cadastrar atleta",
			description = "Cria um novo atleta vinculado ao coach. Retorna 201 Created com o DTO do atleta no corpo "
					+ "e o header Location apontando para o recurso criado.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Atleta criado com sucesso"),
			@ApiResponse(responseCode = "400", description = "Campos inválidos ou ausentes") })
	// T-009/US-037: somente o coach gerencia atletas.
	@PreAuthorize("hasRole('COACH')")
	@PostMapping
	public ResponseEntity<AthleteResponse> register(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody RegisterAthleteRequest request) {
		// T-058: o coachId nunca vem do request — é resolvido server-side a
		// partir do usuário autenticado (subject do JWT) na camada de aplicação.
		Athlete athlete = service.register(new AthleteApplicationService.RegisterAthleteCommand(
				UUID.fromString(jwt.getSubject()), request.name(), request.sex(), request.birthDate(),
				request.height(), request.weight(), request.goal()));
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(athlete.id().value())
				.toUri();
		return ResponseEntity.created(location).body(AthleteResponse.from(athlete));
	}

	@Operation(summary = "Listar atletas da carteira",
			description = "Atletas não removidos do coach autenticado, com filtro opcional por status.")
	@PreAuthorize("hasRole('COACH')")
	@GetMapping
	public List<AthleteResponse> list(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) AthleteStatus status) {
		return service.list(UUID.fromString(jwt.getSubject()), status).stream()
				.map(AthleteResponse::from)
				.toList();
	}

	@Operation(summary = "Ficha do atleta",
			description = "Retorna o atleta; 404 se não existe, foi removido ou é de outro coach.")
	@PreAuthorize("hasRole('COACH')")
	@GetMapping("/{id}")
	public AthleteResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		return AthleteResponse.from(service.get(UUID.fromString(jwt.getSubject()), id));
	}

	@Operation(summary = "Alterar status do atleta",
			description = "Troca o status (Ativo/Pausado/Finalizado) com motivo obrigatório. "
					+ "Trocar para o status atual retorna 409.")
	@PreAuthorize("hasRole('COACH')")
	@PostMapping("/{id}/status")
	public AthleteResponse changeStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
			@Valid @RequestBody ChangeAthleteStatusRequest request) {
		return AthleteResponse.from(service.changeStatus(new AthleteApplicationService.ChangeStatusCommand(
				UUID.fromString(jwt.getSubject()), id, request.status(), request.reason())));
	}

	/**
	 * Remove um atleta da carteira do coach (soft-delete). O registro nunca é
	 * removido fisicamente — apenas marcado como excluído (US-002/UML 4.1.2).
	 */
	@Operation(summary = "Remover atleta (soft-delete)",
			description = "Exclusão lógica do atleta da carteira do coach autenticado: o registro é apenas marcado "
					+ "como excluído e nunca removido fisicamente do banco.")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Atleta removido (soft-delete)"),
			@ApiResponse(responseCode = "404", description = "Atleta não encontrado, removido ou de outro coach") })
	// T-009/US-037: somente o coach gerencia atletas.
	@PreAuthorize("hasRole('COACH')")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> remove(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID id) {
		service.remove(new AthleteApplicationService.RemoveAthleteCommand(
				UUID.fromString(jwt.getSubject()), id));
		return ResponseEntity.noContent().build();
	}
}
