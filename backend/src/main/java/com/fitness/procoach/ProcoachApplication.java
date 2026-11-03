package com.fitness.procoach;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.fitness.procoach")
@EnableJpaRepositories(basePackages = "com.fitness.procoach")
public class ProcoachApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProcoachApplication.class, args);
	}

}
