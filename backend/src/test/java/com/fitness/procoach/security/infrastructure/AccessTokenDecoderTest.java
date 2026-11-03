package com.fitness.procoach.security.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * O resource server só pode aceitar token de acesso emitido pelo ProCoach: refresh
 * token e desafio de MFA são assinados com a mesma chave, mas não valem como Bearer.
 */
class AccessTokenDecoderTest {

	private static final SecretKey KEY = new SecretKeySpec(
			"procoach-test-jwt-secret-0123456789".getBytes(StandardCharsets.UTF_8), "HmacSHA256");

	private final JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(KEY));
	private final JwtDecoder decoder = SecurityConfig.accessTokenDecoder(KEY);

	private String token(String type, String issuer) {
		Instant now = Instant.now();
		JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
				.subject("11111111-1111-1111-1111-111111111111")
				.issuedAt(now)
				.expiresAt(now.plusSeconds(300))
				.claim("role", "COACH");
		if (type != null) {
			claims.claim("type", type);
		}
		if (issuer != null) {
			claims.issuer(issuer);
		}
		return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build()))
				.getTokenValue();
	}

	@Test
	void aceitaTokenDeAcesso() {
		Jwt jwt = decoder.decode(token("access", "procoach"));

		assertThat(jwt.getClaimAsString("type")).isEqualTo("access");
	}

	@Test
	void recusaRefreshToken() {
		assertThatThrownBy(() -> decoder.decode(token("refresh", "procoach"))).isInstanceOf(JwtException.class);
	}

	@Test
	void recusaDesafioDeMfa() {
		assertThatThrownBy(() -> decoder.decode(token("mfa_challenge", "procoach"))).isInstanceOf(JwtException.class);
	}

	@Test
	void recusaTokenSemType() {
		assertThatThrownBy(() -> decoder.decode(token(null, "procoach"))).isInstanceOf(JwtException.class);
	}

	@Test
	void recusaEmissorDiferente() {
		assertThatThrownBy(() -> decoder.decode(token("access", "outro-emissor"))).isInstanceOf(JwtException.class);
	}

}
