package com.fitness.procoach.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitness.procoach.notification.domain.Notification;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationTest {

	private static final Instant NOW = Instant.parse("2026-11-03T12:00:00Z");

	private static Notification notification(String message) {
		return Notification.create(UUID.randomUUID(), "TRAINING_CHANGED", message, "Training", UUID.randomUUID(), NOW);
	}

	@Test
	void nasceNaoLido() {
		Notification n = notification("Seu coach ajustou o treino.");

		assertThat(n.isRead()).isFalse();
		assertThat(n.getReadAt()).isNull();
		assertThat(n.getCreatedAt()).isEqualTo(NOW);
	}

	@Test
	void marcarComoLidoGuardaAData() {
		Notification n = notification("Aviso");

		n.markRead(NOW.plusSeconds(60));

		assertThat(n.isRead()).isTrue();
		assertThat(n.getReadAt()).isEqualTo(NOW.plusSeconds(60));
	}

	@Test
	void marcarDeNovoMantemADataOriginal() {
		Notification n = notification("Aviso");
		n.markRead(NOW.plusSeconds(60));

		n.markRead(NOW.plusSeconds(600));

		assertThat(n.getReadAt()).isEqualTo(NOW.plusSeconds(60));
	}

	@Test
	void mensagemLongaEhTruncadaNoLimite() {
		Notification n = notification("x".repeat(900));

		assertThat(n.getMessage()).hasSize(Notification.MAX_MESSAGE_LENGTH).endsWith("…");
	}

	@Test
	void mensagemEmBrancoEhRecusada() {
		assertThatThrownBy(() -> notification("  ")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void atletaETipoSaoObrigatorios() {
		assertThatThrownBy(() -> Notification.create(null, "TIPO", "msg", null, null, NOW))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> Notification.create(UUID.randomUUID(), " ", "msg", null, null, NOW))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
