package com.fitness.procoach.security.interfaces;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fitness.procoach.security.application.EmailAlreadyRegisteredException;
import com.fitness.procoach.security.application.InvalidCredentialsException;
import com.fitness.procoach.security.application.InvalidMfaCodeException;
import com.fitness.procoach.security.application.InvalidTokenException;
import com.fitness.procoach.security.application.MfaSetupException;

/**
 * Tradução das exceções de autenticação para HTTP (T-008), no mesmo formato de
 * erro dos demais módulos ({@code message} + {@code fields} quando aplicável).
 */
@RestControllerAdvice(assignableTypes = AuthController.class)
public class SecurityExceptionHandler {

	@ExceptionHandler(InvalidCredentialsException.class)
	public ResponseEntity<Map<String, Object>> invalidCredentials(InvalidCredentialsException ex) {
		return error(HttpStatus.UNAUTHORIZED, ex.getMessage());
	}

	@ExceptionHandler(InvalidTokenException.class)
	public ResponseEntity<Map<String, Object>> invalidToken(InvalidTokenException ex) {
		return error(HttpStatus.UNAUTHORIZED, ex.getMessage());
	}

	@ExceptionHandler(InvalidMfaCodeException.class)
	public ResponseEntity<Map<String, Object>> invalidMfaCode(InvalidMfaCodeException ex) {
		return error(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler({ EmailAlreadyRegisteredException.class, MfaSetupException.class })
	public ResponseEntity<Map<String, Object>> conflict(RuntimeException ex) {
		return error(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex) {
		Map<String, String> fields = ex.getBindingResult().getFieldErrors().stream()
				.collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (a, b) -> a,
						LinkedHashMap::new));
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("message", "Campos inválidos ou ausentes");
		body.put("fields", fields);
		return ResponseEntity.badRequest().body(body);
	}

	private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("message", message);
		return ResponseEntity.status(status).body(body);
	}
}
