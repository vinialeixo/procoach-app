package com.fitness.procoach.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitness.procoach.audit.application.AuditData;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditDataTest {

	@Test
	void montaObjetoJsonComPares() {
		assertThat(AuditData.of("from", "ACTIVE", "to", "PAUSED"))
				.isEqualTo("{\"from\":\"ACTIVE\",\"to\":\"PAUSED\"}");
	}

	@Test
	void semParesGeraObjetoVazio() {
		assertThat(AuditData.of()).isEqualTo("{}");
	}

	@Test
	void numerosEBooleanosNaoTemAspas() {
		assertThat(AuditData.of("version", 2, "approved", true)).isEqualTo("{\"version\":2,\"approved\":true}");
	}

	@Test
	void nuloViraJsonNull() {
		assertThat(AuditData.of("previousVersionId", null)).isEqualTo("{\"previousVersionId\":null}");
	}

	@Test
	void outrosTiposUsamToString() {
		UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");

		assertThat(AuditData.of("id", id)).isEqualTo("{\"id\":\"11111111-1111-1111-1111-111111111111\"}");
	}

	@Test
	void escapaAspasBarrasEQuebrasDeLinha() {
		assertThat(AuditData.of("reason", "disse \"oi\"\nlinha2\\fim\t!"))
				.isEqualTo("{\"reason\":\"disse \\\"oi\\\"\\nlinha2\\\\fim\\t!\"}");
	}

	@Test
	void escapaCaracteresDeControle() {
		assertThat(AuditData.of("x", "a\u0001b")).isEqualTo("{\"x\":\"a\\u0001b\"}");
	}

	@Test
	void numeroImparDeArgumentosEhRecusado() {
		assertThatThrownBy(() -> AuditData.of("sozinha")).isInstanceOf(IllegalArgumentException.class);
	}
}
