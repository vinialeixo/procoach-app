package com.fitness.procoach.security.infrastructure;

import com.fitness.procoach.security.application.JwtTokenService;
import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Configuração de segurança do ProCoach (T-008/T-009): API stateless protegida
 * por JWT (HS256). O claim {@code role} vira a autoridade
 * {@code ROLE_COACH}/{@code ROLE_ATHLETE} consumida pelos {@code @PreAuthorize}
 * dos controllers; endpoints de {@code /api/v1/auth/**} e a documentação OpenAPI
 * são públicos.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter,
			SecretKey jwtSecretKey) throws Exception {
		return http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize
						// Públicos: login, cadastro de coach e as etapas autenticadas
						// por token próprio (desafio de MFA / refresh token no corpo).
						// Já /api/v1/auth/mfa/setup e /setup/confirm exigem Bearer válido.
						.requestMatchers("/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/mfa/verify",
								"/api/v1/auth/refresh")
						.permitAll()
						// Convite do atleta: consultar e aceitar são públicos (o token do convite é a
						// credencial); gerar o convite (POST /invites) exige coach autenticado.
						.requestMatchers(HttpMethod.GET, "/api/v1/auth/invites/*").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/invites/*/accept").permitAll()
						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.decoder(accessTokenDecoder(jwtSecretKey))
						.jwtAuthenticationConverter(jwtAuthenticationConverter)))
				.build();
	}

	/**
	 * Decoder usado pelo resource server (Authorization: Bearer): só aceita token
	 * de acesso emitido pelo ProCoach. O refresh token e o desafio de MFA são
	 * assinados com a mesma chave, mas só valem nos respectivos endpoints — que
	 * os validam via {@link JwtTokenService} com o decoder genérico abaixo.
	 */
	static JwtDecoder accessTokenDecoder(SecretKey jwtSecretKey) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
		OAuth2TokenValidator<Jwt> accessOnly = jwt -> JwtTokenService.TYPE_ACCESS.equals(jwt.getClaimAsString("type"))
				? OAuth2TokenValidatorResult.success()
				: OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Token não é de acesso", null));
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
				JwtValidators.createDefaultWithIssuer(JwtTokenService.ISSUER), accessOnly));
		return decoder;
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
		return NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
	}

	@Bean
	JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
	}

	@Bean
	SecretKey jwtSecretKey(@Value("${procoach.security.jwt.secret}") String secret) {
		if (secret == null || secret.isBlank()) {
			throw new IllegalStateException("procoach.security.jwt.secret (PROCOACH_JWT_SECRET) é obrigatório");
		}
		byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
		if (bytes.length < 32) {
			throw new IllegalStateException("procoach.security.jwt.secret precisa de ao menos 32 bytes (HS256)");
		}
		return new SecretKeySpec(bytes, "HmacSHA256");
	}

	@Bean
	JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
		authorities.setAuthoritiesClaimName("role");
		authorities.setAuthorityPrefix("ROLE_");
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authorities);
		return converter;
	}
}
