package com.fitness.procoach.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitness.procoach.notification.domain.DevicePlatform;
import com.fitness.procoach.notification.domain.DeviceToken;
import com.fitness.procoach.notification.domain.Delivery;
import com.fitness.procoach.notification.domain.DeliveryStatus;
import com.fitness.procoach.notification.domain.NotificationChannelType;
import com.fitness.procoach.notification.domain.NotificationPreference;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Regras dos canais externos: preferência, dispositivo e rastro de entrega (T-071). */
class ChannelDomainTest {

	private static final Instant NOW = Instant.parse("2026-11-03T12:00:00Z");

	// ---- preferência ----

	@Test
	void semRegistroTodosOsCanaisEstaoLigados() {
		NotificationPreference preference = NotificationPreference.defaults(UUID.randomUUID());

		assertThat(preference.allows(NotificationChannelType.EMAIL)).isTrue();
		assertThat(preference.allows(NotificationChannelType.PUSH)).isTrue();
	}

	@Test
	void preferenciaDesligaCadaCanalSeparadamente() {
		UUID athleteId = UUID.randomUUID();

		NotificationPreference semEmail = NotificationPreference.of(athleteId, false, true, NOW);
		NotificationPreference semPush = NotificationPreference.of(athleteId, true, false, NOW);

		assertThat(semEmail.allows(NotificationChannelType.EMAIL)).isFalse();
		assertThat(semEmail.allows(NotificationChannelType.PUSH)).isTrue();
		assertThat(semPush.allows(NotificationChannelType.EMAIL)).isTrue();
		assertThat(semPush.allows(NotificationChannelType.PUSH)).isFalse();
	}

	@Test
	void preferenciaExigeAtleta() {
		assertThatThrownBy(() -> NotificationPreference.of(null, true, true, NOW))
				.isInstanceOf(NullPointerException.class);
	}

	// ---- dispositivo ----

	@Test
	void registroRepetidoMudaDonoPlataformaEUltimoUso() {
		UUID first = UUID.randomUUID();
		UUID second = UUID.randomUUID();
		DeviceToken device = DeviceToken.register(first, "token-abc", DevicePlatform.ANDROID, NOW);

		device.seenBy(second, DevicePlatform.IOS, NOW.plusSeconds(60));

		assertThat(device.getAthleteId()).isEqualTo(second);
		assertThat(device.getPlatform()).isEqualTo(DevicePlatform.IOS);
		assertThat(device.getLastSeenAt()).isEqualTo(NOW.plusSeconds(60));
		assertThat(device.getCreatedAt()).isEqualTo(NOW);
		assertThat(device.getToken()).isEqualTo("token-abc");
	}

	@Test
	void dispositivoExigeTokenPreenchido() {
		assertThatThrownBy(() -> DeviceToken.register(UUID.randomUUID(), " ", DevicePlatform.WEB, NOW))
				.isInstanceOf(IllegalArgumentException.class);
	}

	// ---- entrega ----

	private static Delivery delivery() {
		return Delivery.start(UUID.randomUUID(), NotificationChannelType.EMAIL, NOW);
	}

	@Test
	void entregaNasceSemTentativas() {
		Delivery d = delivery();

		assertThat(d.getStatus()).isEqualTo(DeliveryStatus.PENDING);
		assertThat(d.getAttempts()).isZero();
		assertThat(d.isFinal()).isFalse();
		assertThat(d.canRetry()).isFalse();
	}

	@Test
	void envioConcluidoEhFinalEContaUmaTentativa() {
		Delivery d = delivery();

		d.markSent(NOW.plusSeconds(1));

		assertThat(d.getStatus()).isEqualTo(DeliveryStatus.SENT);
		assertThat(d.getAttempts()).isEqualTo(1);
		assertThat(d.isFinal()).isTrue();
		assertThat(d.canRetry()).isFalse();
		assertThat(d.getLastError()).isNull();
	}

	@Test
	void entregaSemDestinoEhFinalENaoRepete() {
		Delivery d = delivery();

		d.markSkipped(NOW, "atleta sem e-mail");

		assertThat(d.getStatus()).isEqualTo(DeliveryStatus.SKIPPED);
		assertThat(d.isFinal()).isTrue();
		assertThat(d.canRetry()).isFalse();
		assertThat(d.getLastError()).isEqualTo("atleta sem e-mail");
	}

	@Test
	void falhaPodeSerRepetidaAteOLimite() {
		Delivery d = delivery();

		d.markFailed(NOW, "timeout");
		assertThat(d.canRetry()).isTrue();
		d.markFailed(NOW, "timeout");
		assertThat(d.canRetry()).isTrue();
		d.markFailed(NOW, "timeout");

		assertThat(d.getAttempts()).isEqualTo(Delivery.MAX_ATTEMPTS);
		assertThat(d.getStatus()).isEqualTo(DeliveryStatus.FAILED);
		assertThat(d.canRetry()).isFalse();
	}

	@Test
	void repeticaoQueDaCertoFechaAEntrega() {
		Delivery d = delivery();
		d.markFailed(NOW, "timeout");

		d.markSent(NOW.plusSeconds(300));

		assertThat(d.getStatus()).isEqualTo(DeliveryStatus.SENT);
		assertThat(d.getAttempts()).isEqualTo(2);
		assertThat(d.getLastError()).isNull();
	}

	@Test
	void entregaFinalNaoAceitaNovaTentativa() {
		Delivery d = delivery();
		d.markSent(NOW);

		assertThatThrownBy(() -> d.markFailed(NOW, "x")).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> d.markSent(NOW)).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void erroLongoEhTruncado() {
		Delivery d = delivery();

		d.markFailed(NOW, "x".repeat(900));

		assertThat(d.getLastError()).hasSize(500);
	}
}
