package com.fitness.procoach;

import org.springframework.boot.SpringApplication;

public class TestProcoachApplication {

	public static void main(String[] args) {
		SpringApplication.from(ProcoachApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
