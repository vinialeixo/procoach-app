package com.fitness.procoach.notification.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitness.procoach.notification.application.NotificationChannel;
import com.fitness.procoach.notification.domain.DevicePlatform;
import com.fitness.procoach.notification.domain.DeviceToken;
import com.fitness.procoach.notification.domain.DeviceTokenRepository;
import com.fitness.procoach.notification.domain.Notification;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/** Canais de e-mail e push: disponibilidade, destino ausente, conteúdo e falha. */
class ExternalChannelsTest {

	private final UUID athleteId = UUID.randomUUID();
	private final Notification notification = Notification.create(athleteId, "TRAINING_CHANGED",
			"Seu coach ajustou \"Supino\" no treino \"Treino A\": séries 4 → 5.", "Training", UUID.randomUUID(),
			Instant.now());

	// ---- e-mail ----

	private static final class RecordingMailSender extends JavaMailSenderImpl {
		final List<SimpleMailMessage> sent = new ArrayList<>();
		RuntimeException failure;

		@Override
		public void send(SimpleMailMessage... messages) {
			if (failure != null) {
				throw failure;
			}
			sent.addAll(List.of(messages));
		}
	}

	private static <T> ObjectProvider<T> provider(Supplier<T> supplier) {
		return new ObjectProvider<>() {
			@Override
			public T getObject(Object... args) {
				return supplier.get();
			}

			@Override
			public T getObject() {
				return supplier.get();
			}

			@Override
			public T getIfAvailable() {
				return supplier.get();
			}

			@Override
			public T getIfUnique() {
				return supplier.get();
			}
		};
	}

	private EmailNotificationChannel email(boolean enabled, JavaMailSender sender, String address) {
		return new EmailNotificationChannel(provider(() -> sender), id -> Optional.ofNullable(address), enabled,
				"no-reply@procoach.test");
	}

	@Test
	void emailDesligadoPorPadraoFicaIndisponivel() {
		assertThat(email(false, new RecordingMailSender(), "ana@exemplo.com").available()).isFalse();
	}

	@Test
	void emailLigadoMasSemServidorSmtpFicaIndisponivel() {
		EmailNotificationChannel channel = new EmailNotificationChannel(provider(() -> null),
				id -> Optional.of("ana@exemplo.com"), true, "no-reply@procoach.test");

		assertThat(channel.available()).isFalse();
	}

	@Test
	void emailLigadoComServidorFicaDisponivel() {
		assertThat(email(true, new RecordingMailSender(), "ana@exemplo.com").available()).isTrue();
	}

	@Test
	void enviaOTextoDoAvisoParaOEmailDeAcessoDoAtleta() {
		RecordingMailSender sender = new RecordingMailSender();

		NotificationChannel.Result result = email(true, sender, "ana@exemplo.com").send(notification);

		assertThat(result.skipped()).isFalse();
		assertThat(sender.sent).hasSize(1);
		SimpleMailMessage message = sender.sent.get(0);
		assertThat(message.getTo()).containsExactly("ana@exemplo.com");
		assertThat(message.getFrom()).isEqualTo("no-reply@procoach.test");
		assertThat(message.getSubject()).isEqualTo(EmailNotificationChannel.SUBJECT);
		assertThat(message.getText()).contains("séries 4 → 5");
	}

	@Test
	void atletaSemEmailDeAcessoFicaSkipped() {
		RecordingMailSender sender = new RecordingMailSender();

		NotificationChannel.Result result = email(true, sender, null).send(notification);

		assertThat(result.skipped()).isTrue();
		assertThat(result.detail()).contains("sem e-mail");
		assertThat(sender.sent).isEmpty();
	}

	@Test
	void falhaDoServidorSmtpPropagaParaOsDespachanteRegistrar() {
		RecordingMailSender sender = new RecordingMailSender();
		sender.failure = new MailSendException("smtp fora do ar");

		assertThatThrownBy(() -> email(true, sender, "ana@exemplo.com").send(notification))
				.isInstanceOf(MailSendException.class);
	}

	// ---- push ----

	private static final class InMemoryDevices implements DeviceTokenRepository {
		final List<DeviceToken> store = new ArrayList<>();

		@Override
		public DeviceToken save(DeviceToken device) {
			store.add(device);
			return device;
		}

		@Override
		public Optional<DeviceToken> findByToken(String token) {
			return store.stream().filter(d -> d.getToken().equals(token)).findFirst();
		}

		@Override
		public List<DeviceToken> findByAthleteId(UUID athleteId) {
			return store.stream().filter(d -> d.getAthleteId().equals(athleteId)).toList();
		}

		@Override
		public void delete(DeviceToken device) {
			store.remove(device);
		}
	}

	private static final class RecordingGateway implements PushGateway {
		final List<String> tokens = new ArrayList<>();
		final List<String> bodies = new ArrayList<>();
		final List<String> failingTokens = new ArrayList<>();

		@Override
		public void send(String deviceToken, String title, String body) {
			if (failingTokens.contains(deviceToken)) {
				throw new IllegalStateException("token inválido");
			}
			tokens.add(deviceToken);
			bodies.add(body);
		}
	}

	private PushNotificationChannel push(RecordingGateway gateway, InMemoryDevices devices) {
		return new PushNotificationChannel(provider(() -> gateway), devices);
	}

	@Test
	void pushSemProvedorFicaIndisponivel() {
		PushNotificationChannel channel = new PushNotificationChannel(provider(() -> null), new InMemoryDevices());

		assertThat(channel.available()).isFalse();
	}

	@Test
	void pushSemDispositivoFicaSkipped() {
		RecordingGateway gateway = new RecordingGateway();

		NotificationChannel.Result result = push(gateway, new InMemoryDevices()).send(notification);

		assertThat(result.skipped()).isTrue();
		assertThat(result.detail()).contains("sem dispositivo");
		assertThat(gateway.tokens).isEmpty();
	}

	@Test
	void pushVaiParaTodosOsDispositivosDoAtletaEPNaoParaOsDeOutros() {
		InMemoryDevices devices = new InMemoryDevices();
		devices.save(DeviceToken.register(athleteId, "tok-celular", DevicePlatform.ANDROID, Instant.now()));
		devices.save(DeviceToken.register(athleteId, "tok-tablet", DevicePlatform.IOS, Instant.now()));
		devices.save(DeviceToken.register(UUID.randomUUID(), "tok-de-outro", DevicePlatform.WEB, Instant.now()));
		RecordingGateway gateway = new RecordingGateway();

		NotificationChannel.Result result = push(gateway, devices).send(notification);

		assertThat(result.skipped()).isFalse();
		assertThat(gateway.tokens).containsExactlyInAnyOrder("tok-celular", "tok-tablet");
		assertThat(gateway.bodies).allMatch(body -> body.contains("séries 4 → 5"));
	}

	@Test
	void pushContaComoEnviadoSeAoMenosUmDispositivoRecebeu() {
		InMemoryDevices devices = new InMemoryDevices();
		devices.save(DeviceToken.register(athleteId, "tok-bom", DevicePlatform.ANDROID, Instant.now()));
		devices.save(DeviceToken.register(athleteId, "tok-ruim", DevicePlatform.IOS, Instant.now()));
		RecordingGateway gateway = new RecordingGateway();
		gateway.failingTokens.add("tok-ruim");

		NotificationChannel.Result result = push(gateway, devices).send(notification);

		assertThat(result.skipped()).isFalse();
		assertThat(gateway.tokens).containsExactly("tok-bom");
	}

	@Test
	void pushFalhaQuandoTodosOsDispositivosRecusam() {
		InMemoryDevices devices = new InMemoryDevices();
		devices.save(DeviceToken.register(athleteId, "tok-ruim", DevicePlatform.IOS, Instant.now()));
		RecordingGateway gateway = new RecordingGateway();
		gateway.failingTokens.add("tok-ruim");

		assertThatThrownBy(() -> push(gateway, devices).send(notification))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("token inválido");
	}
}
