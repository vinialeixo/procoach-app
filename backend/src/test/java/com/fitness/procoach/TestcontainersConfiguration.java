package com.fitness.procoach;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Configuração de Testcontainers: sobe um PostgreSQL real para os testes de
 * integração. O @ServiceConnection injeta automaticamente o datasource e o
 * Flyway roda as migrações contra o container.
 */
// Com o profile h2 (./mvnw test -Dspring.profiles.active=h2) os testes usam H2 em memória, sem Docker.
@Profile("!h2")
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer<?> postgresContainer() {
		return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));
	}
}
