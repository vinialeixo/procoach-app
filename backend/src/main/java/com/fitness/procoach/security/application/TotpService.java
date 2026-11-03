package com.fitness.procoach.security.application;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

/**
 * TOTP (RFC 6238) com HMAC-SHA1, passo de 30s e código de 6 dígitos —
 * compatível com Google Authenticator, Authy, 1Password etc. O segredo é
 * codificado em Base32 (RFC 4648, sem padding), que é o formato esperado
 * pelos aplicativos autenticadores.
 */
@Service
public class TotpService {

	private static final int SECRET_BYTES = 20;
	private static final int TIME_STEP_SECONDS = 30;
	private static final int CODE_DIGITS = 6;
	private static final int VERIFICATION_WINDOW_STEPS = 1;
	private static final String HMAC_ALGORITHM = "HmacSHA1";
	private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

	private final SecureRandom random = new SecureRandom();

	public String generateSecret() {
		byte[] bytes = new byte[SECRET_BYTES];
		random.nextBytes(bytes);
		return base32Encode(bytes);
	}

	public String otpauthUrl(String issuer, String account, String base32Secret) {
		return "otpauth://totp/" + issuer + ":" + account
				+ "?secret=" + base32Secret + "&issuer=" + issuer
				+ "&digits=" + CODE_DIGITS + "&period=" + TIME_STEP_SECONDS;
	}

	/**
	 * Valida o código de 6 dígitos aceitando uma janela de ±1 passo de 30s para
	 * tolerar o descompasso de relógio entre servidor e autenticador.
	 */
	public boolean verify(String base32Secret, String code) {
		if (base32Secret == null || code == null || !code.matches("\\d{6}")) {
			return false;
		}
		long step = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;
		for (long offset = -VERIFICATION_WINDOW_STEPS; offset <= VERIFICATION_WINDOW_STEPS; offset++) {
			if (constantTimeEquals(code, codeForStep(base32Secret, step + offset))) {
				return true;
			}
		}
		return false;
	}

	/** Código do passo atual. Exposto para os testes de integração do MFA. */
	public String currentCode(String base32Secret) {
		return codeForStep(base32Secret, Instant.now().getEpochSecond() / TIME_STEP_SECONDS);
	}

	private String codeForStep(String base32Secret, long step) {
		byte[] key = base32Decode(base32Secret);
		byte[] counter = new byte[8];
		for (int i = 7; i >= 0; i--) {
			counter[i] = (byte) (step & 0xFF);
			step >>= 8;
		}
		try {
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
			byte[] hash = mac.doFinal(counter);
			int offset = hash[hash.length - 1] & 0x0F;
			int binary = ((hash[offset] & 0x7F) << 24)
					| ((hash[offset + 1] & 0xFF) << 16)
					| ((hash[offset + 2] & 0xFF) << 8)
					| (hash[offset + 3] & 0xFF);
			int code = binary % (int) Math.pow(10, CODE_DIGITS);
			return String.format("%0" + CODE_DIGITS + "d", code);
		}
		catch (GeneralSecurityException e) {
			throw new IllegalStateException("Algoritmo HMAC-SHA1 indisponível", e);
		}
	}

	private boolean constantTimeEquals(String a, String b) {
		if (a.length() != b.length()) {
			return false;
		}
		int result = 0;
		for (int i = 0; i < a.length(); i++) {
			result |= a.charAt(i) ^ b.charAt(i);
		}
		return result == 0;
	}

	private String base32Encode(byte[] data) {
		StringBuilder out = new StringBuilder((data.length * 8 + 4) / 5);
		int buffer = 0;
		int bitsLeft = 0;
		for (byte b : data) {
			buffer = (buffer << 8) | (b & 0xFF);
			bitsLeft += 8;
			while (bitsLeft >= 5) {
				out.append(BASE32_ALPHABET.charAt((buffer >> (bitsLeft - 5)) & 0x1F));
				bitsLeft -= 5;
			}
		}
		if (bitsLeft > 0) {
			out.append(BASE32_ALPHABET.charAt((buffer << (5 - bitsLeft)) & 0x1F));
		}
		return out.toString();
	}

	private byte[] base32Decode(String encoded) {
		String normalized = encoded.toUpperCase().replace("=", "").replaceAll("\\s", "");
		int buffer = 0;
		int bitsLeft = 0;
		byte[] out = new byte[normalized.length() * 5 / 8];
		int index = 0;
		for (char c : normalized.toCharArray()) {
			int value = BASE32_ALPHABET.indexOf(c);
			if (value < 0) {
				throw new IllegalArgumentException("Segredo Base32 inválido");
			}
			buffer = (buffer << 5) | value;
			bitsLeft += 5;
			if (bitsLeft >= 8) {
				out[index++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
				bitsLeft -= 8;
			}
		}
		return out;
	}
}
