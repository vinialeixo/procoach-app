package com.fitness.procoach.nutrition.interfaces;

import com.fitness.procoach.athlete.application.AthleteNotFoundException;
import com.fitness.procoach.nutrition.application.DietNotFoundException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Tratamento de exceções do módulo nutrition. Erros de validação retornam
 * {@code 400 Bad Request} detalhando os campos inválidos/ausentes; dieta
 * inexistente retorna {@code 404 Not Found}.
 */
@RestControllerAdvice(assignableTypes = DietController.class)
public class NutritionExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
		Map<String, String> fields = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("timestamp", Instant.now());
		body.put("status", 400);
		body.put("error", "Campos inválidos ou ausentes");
		body.put("fields", fields);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

	/** Regras de domínio violadas (ex.: macronutrientes não positivos). */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, Object>> handleDomainViolation(IllegalArgumentException ex) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("timestamp", Instant.now());
		body.put("status", 400);
		body.put("error", ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

	@ExceptionHandler(DietNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleNotFound(DietNotFoundException ex) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("timestamp", Instant.now());
		body.put("status", 404);
		body.put("error", ex.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
	}

	/** Atleta inexistente, removido ou de outro coach (T-058) — 404, não vaza existência. */
	@ExceptionHandler(AthleteNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleAthleteNotFound(AthleteNotFoundException ex) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("timestamp", Instant.now());
		body.put("status", 404);
		body.put("error", ex.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
	}

	/** Corpo da requisição malformado ou valores fora do domínio. */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Map<String, Object>> handleUnreadableBody(HttpMessageNotReadableException ex) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("timestamp", Instant.now());
		body.put("status", 400);
		body.put("error", "Corpo da requisição inválido");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}
}
