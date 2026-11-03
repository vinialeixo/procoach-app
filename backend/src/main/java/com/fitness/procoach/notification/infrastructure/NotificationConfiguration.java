package com.fitness.procoach.notification.infrastructure;

import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Infraestrutura da entrega externa: executor dedicado (fora da requisição do coach) e agendamento
 * da repetição das entregas que falharam.
 */
@Configuration
@EnableScheduling
public class NotificationConfiguration {

	/**
	 * Executor da entrega. Com {@code procoach.notification.async=false} (testes) roda na própria
	 * thread, para o resultado ser determinístico.
	 */
	@Bean(name = "notificationExecutor")
	public Executor notificationExecutor(@Value("${procoach.notification.async:true}") boolean async) {
		if (!async) {
			return Runnable::run;
		}
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("notification-");
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(4);
		executor.setQueueCapacity(200);
		executor.initialize();
		return executor;
	}
}
