package com.fitness.procoach.training.interfaces.rest;

import java.util.Map;

import com.fitness.procoach.athlete.application.AthleteNotFoundException;
import com.fitness.procoach.training.application.TrainingResourceNotFoundException;
import java.util.LinkedHashMap;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.fitness.procoach.training")
public class TrainingExceptionHandler {

	/** Treino, plano ou periodização inexistente (ou de outro atleta) — 404. */
	@ExceptionHandler(TrainingResourceNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleNotFound(TrainingResourceNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
	}

	/** Bean Validation dos DTOs de entrada. */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
		Map<String, String> fields = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors()
			.forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(Map.of("error", "Campos inválidos ou ausentes", "fields", fields));
	}

	/** Violação de UNIQUE (ex.: duas versões a partir da mesma origem, em corrida) — 409. */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<Map<String, String>> handleConflict(DataIntegrityViolationException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
			.body(Map.of("error", "Conflito: o recurso foi alterado por outra requisição. Tente novamente."));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
	}

	/** Atleta inexistente, removido ou de outro coach (T-058) — 404, não vaza existência. */
	@ExceptionHandler(AthleteNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleAthleteNotFound(AthleteNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
	}

}
