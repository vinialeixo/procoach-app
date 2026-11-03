package com.fitness.procoach.training;

import com.fitness.procoach.ProcoachApplication;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class TrainingModuleBoundaryTest {

	@Test
	void verifiesModuleBoundaries() {
		ApplicationModules modules = ApplicationModules.of(ProcoachApplication.class);
		modules.verify();
	}

}
