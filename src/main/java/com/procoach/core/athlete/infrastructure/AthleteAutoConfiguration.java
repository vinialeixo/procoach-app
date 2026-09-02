package com.procoach.core.athlete.infrastructure;

import com.procoach.core.athlete.domain.AthleteRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Auto-configuração do módulo athlete. Registra o scan de entidades e
 * repositórios JPA do pacote {@code com.procoach.core.athlete} e expõe o
 * adapter da porta {@link AthleteRepository} como bean, sem depender do
 * component scan do pacote raiz da aplicação.
 */
@AutoConfiguration
@AutoConfigureBefore(HibernateJpaAutoConfiguration.class)
@EntityScan(basePackages = "com.procoach.core.athlete")
@EnableJpaRepositories(basePackages = "com.procoach.core.athlete")
public class AthleteAutoConfiguration {

	@Bean
	AthleteRepository athleteRepository(AthleteJpaRepository jpaRepository) {
		return new AthleteRepositoryAdapter(jpaRepository);
	}
}
