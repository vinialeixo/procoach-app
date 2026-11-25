package com.fitness.procoach.notification.infrastructure;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Credenciais de uma conta de serviço do Google (FCM HTTP v1). {@code credentials} aceita o
 * caminho de um arquivo JSON ou o próprio JSON (útil para injetar o segredo por variável de
 * ambiente); o projeto pode ser sobrescrito por configuração.
 */
record GoogleServiceAccount(String clientEmail, PrivateKey privateKey, String tokenUri, String projectId) {

	private static final String DEFAULT_TOKEN_URI = "https://oauth2.googleapis.com/token";

	private static final ObjectMapper MAPPER = JsonMapper.builder().build();

	static GoogleServiceAccount load(String credentials, String projectIdOverride) {
		if (credentials == null || credentials.isBlank()) {
			throw new IllegalStateException(
					"credencial do FCM ausente: defina procoach.notification.push.fcm.credentials "
							+ "(caminho do JSON da conta de serviço ou o próprio JSON)");
		}
		String json = read(credentials.trim());
		JsonNode root;
		try {
			root = MAPPER.readTree(json);
		}
		catch (RuntimeException ex) {
			throw new IllegalStateException("credencial do FCM não é um JSON válido", ex);
		}
		String clientEmail = required(root, "client_email");
		String projectId = projectIdOverride != null && !projectIdOverride.isBlank() ? projectIdOverride
				: required(root, "project_id");
		String tokenUri = root.hasNonNull("token_uri") ? root.get("token_uri").asText() : DEFAULT_TOKEN_URI;
		return new GoogleServiceAccount(clientEmail, privateKey(required(root, "private_key")), tokenUri, projectId);
	}

	private static String read(String credentials) {
		String trimmed = credentials.trim();
		if (trimmed.startsWith("{")) {
			return trimmed;
		}
		try {
			return Files.readString(Path.of(trimmed), StandardCharsets.UTF_8);
		}
		catch (IOException ex) {
			throw new IllegalStateException("não foi possível ler a credencial do FCM em " + trimmed, ex);
		}
	}

	private static String required(JsonNode root, String field) {
		JsonNode value = root.get(field);
		if (value == null || value.asText().isBlank()) {
			throw new IllegalStateException("credencial do FCM sem o campo " + field);
		}
		return value.asText();
	}

	private static PrivateKey privateKey(String pem) {
		String base64 = pem.replace("-----BEGIN PRIVATE KEY-----", "")
			.replace("-----END PRIVATE KEY-----", "")
			.replace("-----BEGIN RSA PRIVATE KEY-----", "")
			.replace("-----END RSA PRIVATE KEY-----", "")
			.replaceAll("\\s", "");
		try {
			byte[] der = Base64.getDecoder().decode(base64);
			return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
		}
		catch (IllegalArgumentException | GeneralSecurityException ex) {
			throw new IllegalStateException("chave privada da conta de serviço do FCM inválida", ex);
		}
	}
}
