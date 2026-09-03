package com.fitness.procoach.athlete.infrastructure;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Sobe um PostgreSQL real via Testcontainers para os testes de integração
 * do módulo athlete.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer<?> postgres() {
		return new PostgreSQLContainer<>("postgres:16-alpine");
	}
}
