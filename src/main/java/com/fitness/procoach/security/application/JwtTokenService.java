package com.fitness.procoach.security.application;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import com.fitness.procoach.security.domain.User;

/**
 * Emissão e validação dos JWTs do ProCoach (T-008). Três tipos de token,
 * distinguidos pelo claim {@code type}:
 *
 * <ul>
 * <li>{@code access} — curto (15min), carrega {@code role} e, para atletas,
 * {@code athleteId} (âncora do isolamento de dados da T-010);</li>
 * <li>{@code refresh} — longo (7 dias), só serve para renovar o par;</li>
 * <li>{@code mfa_challenge} — curtíssimo (5min), prova de que a senha já foi
 * validada e só autoriza a chamada de verificação do código TOTP.</li>
 * </ul>
 */
@Service
public class JwtTokenService {

	static final String CLAIM_TYPE = "type";
	static final String TYPE_ACCESS = "access";
	static final String TYPE_REFRESH = "refresh";
	static final String TYPE_MFA_CHALLENGE = "mfa_challenge";
	static final String ISSUER = "procoach";

	private final JwtEncoder encoder;
	private final JwtDecoder decoder;
	private final Duration accessTokenTtl;
	private final Duration refreshTokenTtl;
	private final Duration mfaChallengeTtl;

	public JwtTokenService(JwtEncoder encoder, JwtDecoder decoder,
			@Value("${procoach.security.jwt.access-token-ttl}") Duration accessTokenTtl,
			@Value("${procoach.security.jwt.refresh-token-ttl}") Duration refreshTokenTtl,
			@Value("${procoach.security.jwt.mfa-challenge-ttl}") Duration mfaChallengeTtl) {
		this.encoder = encoder;
		this.decoder = decoder;
		this.accessTokenTtl = accessTokenTtl;
		this.refreshTokenTtl = refreshTokenTtl;
		this.mfaChallengeTtl = mfaChallengeTtl;
	}

	public String issueAccessToken(User user, String role) {
		JwtClaimsSet.Builder claims = baseClaims(user, accessTokenTtl)
				.claim(CLAIM_TYPE, TYPE_ACCESS)
				.claim("role", role);
		if (user.getAthleteId() != null) {
			claims.claim("athleteId", user.getAthleteId().toString());
		}
		return encode(claims.build());
	}

	public String issueRefreshToken(User user) {
		return encode(baseClaims(user, refreshTokenTtl).claim(CLAIM_TYPE, TYPE_REFRESH).build());
	}

	public String issueMfaChallenge(User user) {
		return encode(baseClaims(user, mfaChallengeTtl).claim(CLAIM_TYPE, TYPE_MFA_CHALLENGE).build());
	}

	public long accessTokenExpiresInSeconds() {
		return accessTokenTtl.toSeconds();
	}

	/** Decodifica e valida assinatura/expiração. Lança {@link InvalidTokenException} se inválido. */
	public Jwt parse(String token) {
		try {
			return decoder.decode(token);
		}
		catch (JwtException | IllegalArgumentException e) {
			throw new InvalidTokenException("Token inválido ou expirado");
		}
	}

	public void requireType(Jwt jwt, String expectedType) {
		if (!expectedType.equals(jwt.getClaimAsString(CLAIM_TYPE))) {
			throw new InvalidTokenException("Tipo de token inesperado para esta operação");
		}
	}

	private JwtClaimsSet.Builder baseClaims(User user, Duration ttl) {
		Instant now = Instant.now();
		return JwtClaimsSet.builder()
				.issuer(ISSUER)
				.issuedAt(now)
				.expiresAt(now.plus(ttl))
				.subject(user.getId().toString());
	}

	private String encode(JwtClaimsSet claims) {
		// Especifica HS256 explicitamente: o NimbusJwtEncoder precisa saber o
		// algoritmo para selecionar a chave do ImmutableSecret (T-008).
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}
}
