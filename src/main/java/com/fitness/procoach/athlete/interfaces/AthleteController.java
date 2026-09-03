package com.fitness.procoach.athlete.interfaces;

import com.fitness.procoach.athlete.application.AthleteApplicationService;
import com.fitness.procoach.athlete.domain.Athlete;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Controller REST do cadastro de atletas (T-018/US-001).
 */
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
	@PostMapping
	public ResponseEntity<AthleteResponse> register(@Valid @RequestBody RegisterAthleteRequest request) {
		Athlete athlete = service.register(new AthleteApplicationService.RegisterAthleteCommand(
				request.coachId(), request.name(), request.sex(), request.birthDate(),
				request.height(), request.weight(), request.goal()));
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(athlete.id().value())
				.toUri();
		return ResponseEntity.created(location).body(AthleteResponse.from(athlete));
	}
}
