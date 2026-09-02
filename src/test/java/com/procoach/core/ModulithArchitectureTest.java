package com.procoach.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulithArchitectureTest {

	@Test
	void verifiesModuleBoundaries() {
		ApplicationModules modules = ApplicationModules.of("com.procoach.core");
		modules.verify();

		assertThat(modules.stream().map(m -> m.getBasePackage().getLocalName()))
				.contains("security", "audit");
	}
}
