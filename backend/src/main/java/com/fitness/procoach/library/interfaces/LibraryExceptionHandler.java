package com.fitness.procoach.library.interfaces;

import java.time.Instant;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fitness.procoach.library.application.ExerciseNotFoundException;

/** Tratamento de exceções do módulo library. */
@RestControllerAdvice(assignableTypes = ExerciseController.class)
public class LibraryExceptionHandler {

	@ExceptionHandler(ExerciseNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleNotFound(ExerciseNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(Map.of("timestamp", Instant.now(), "status", 404, "error", ex.getMessage()));
	}

	/** Regras de domínio violadas (ex.: nome vazio, referência inválida). */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, Object>> handleDomainViolation(IllegalArgumentException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(Map.of("timestamp", Instant.now(), "status", 400, "error", ex.getMessage()));
	}

	/** Versão já substituída ou exercício removido (ex.: editar uma versão antiga). */
	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<Map<String, Object>> handleConflict(IllegalStateException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(Map.of("timestamp", Instant.now(), "status", 409, "error", ex.getMessage()));
	}

	/** Duas edições simultâneas da mesma versão: a cadeia não bifurca (UNIQUE em previous_version_id). */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<Map<String, Object>> handleIntegrityViolation(DataIntegrityViolationException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("timestamp", Instant.now(), "status", 409,
				"error", "Conflito: o exercício foi alterado por outra requisição. Tente novamente."));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(Map.of("timestamp", Instant.now(), "status", 400,
						"error", "Requisição inválida", "details", ex.getBindingResult().getAllErrors()));
	}
}
