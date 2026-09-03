package com.fitness.procoach.athlete.interfaces;

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
 * Tratamento de exceções do módulo athlete (T-018). Erros de validação
 * (Bean Validation ou regras de domínio) retornam {@code 400 Bad Request}
 * detalhando os campos inválidos/ausentes.
 */
@RestControllerAdvice(assignableTypes = AthleteController.class)
public class AthleteExceptionHandler {

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

	/** Regras de domínio violadas (ex.: data de nascimento futura, peso/altura não positivos). */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, Object>> handleDomainViolation(IllegalArgumentException ex) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("timestamp", Instant.now());
		body.put("status", 400);
		body.put("error", ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

	/** Corpo da requisição malformado ou valores fora do domínio (ex.: enum inexistente). */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Map<String, Object>> handleUnreadableBody(HttpMessageNotReadableException ex) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("timestamp", Instant.now());
		body.put("status", 400);
		body.put("error", "Corpo da requisição inválido");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}
}
