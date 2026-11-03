package com.fitness.procoach.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitness.procoach.notification.application.NotificationChannel;
import com.fitness.procoach.notification.application.NotificationDispatcher;
import com.fitness.procoach.notification.domain.Delivery;
import com.fitness.procoach.notification.domain.DeliveryRepository;
import com.fitness.procoach.notification.domain.DeliveryStatus;
import com.fitness.procoach.notification.domain.Notification;
import com.fitness.procoach.notification.domain.NotificationChannelType;
import com.fitness.procoach.notification.domain.NotificationPreference;
import com.fitness.procoach.notification.domain.NotificationPreferenceRepository;
import com.fitness.procoach.notification.domain.NotificationRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Despachante: preferência, canal indisponível, falha isolada, repetição e limite de tentativas. */
class NotificationDispatcherTest {

	private final UUID athleteId = UUID.randomUUID();
	private final InMemoryNotifications notifications = new InMemoryNotifications();
	private final InMemoryPreferences preferences = new InMemoryPreferences();
	private final InMemoryDeliveries deliveries = new InMemoryDeliveries();
	private FakeChannel email;
	private FakeChannel push;
	private NotificationDispatcher dispatcher;
	private Notification notification;

	@BeforeEach
	void setUp() {
		email = new FakeChannel(NotificationChannelType.EMAIL);
		push = new FakeChannel(NotificationChannelType.PUSH);
		dispatcher = new NotificationDispatcher(notifications, preferences, deliveries, List.of(email, push));
		notification = notifications.save(
				Notification.create(athleteId, "TRAINING_CHANGED", "Seu coach ajustou o treino.", "Training",
						UUID.randomUUID(), Instant.now()));
	}

	private Delivery delivery(NotificationChannelType channel) {
		return deliveries.find(notification.getId(), channel).orElseThrow();
	}

	@Test
	void entregaPelosDoisCanaisQuandoTudoEstaLigado() {
		dispatcher.dispatch(notification.getId());

		assertThat(email.sent).containsExactly(notification.getId());
		assertThat(push.sent).containsExactly(notification.getId());
		assertThat(delivery(NotificationChannelType.EMAIL).getStatus()).isEqualTo(DeliveryStatus.SENT);
		assertThat(delivery(NotificationChannelType.PUSH).getStatus()).isEqualTo(DeliveryStatus.SENT);
	}

	@Test
	void respeitaAPreferenciaDoAtleta() {
		preferences.save(NotificationPreference.of(athleteId, false, true, Instant.now()));

		dispatcher.dispatch(notification.getId());

		assertThat(email.sent).isEmpty();
		assertThat(deliveries.find(notification.getId(), NotificationChannelType.EMAIL)).isEmpty();
		assertThat(push.sent).hasSize(1);
	}

	@Test
	void canalIndisponivelEIgnoradoSemRegistrarEntrega() {
		push.available = false;

		dispatcher.dispatch(notification.getId());

		assertThat(push.sent).isEmpty();
		assertThat(deliveries.find(notification.getId(), NotificationChannelType.PUSH)).isEmpty();
		assertThat(email.sent).hasSize(1);
	}

	@Test
	void falhaDeUmCanalNaoImpedeOOutro() {
		email.failures = 1;

		dispatcher.dispatch(notification.getId());

		Delivery failed = delivery(NotificationChannelType.EMAIL);
		assertThat(failed.getStatus()).isEqualTo(DeliveryStatus.FAILED);
		assertThat(failed.getAttempts()).isEqualTo(1);
		assertThat(failed.getLastError()).contains("RuntimeException").contains("smtp fora do ar");
		assertThat(push.sent).hasSize(1);
		assertThat(delivery(NotificationChannelType.PUSH).getStatus()).isEqualTo(DeliveryStatus.SENT);
	}

	@Test
	void semDestinoFicaComoSkippedESemRepeticao() {
		email.skip = "atleta sem e-mail de acesso";

		dispatcher.dispatch(notification.getId());
		int retried = dispatcher.retryFailed();

		Delivery skipped = delivery(NotificationChannelType.EMAIL);
		assertThat(skipped.getStatus()).isEqualTo(DeliveryStatus.SKIPPED);
		assertThat(skipped.getLastError()).isEqualTo("atleta sem e-mail de acesso");
		assertThat(retried).isZero();
		assertThat(email.sent).isEmpty();
	}

	@Test
	void repeticaoEntregaOQueFalhou() {
		email.failures = 1;
		dispatcher.dispatch(notification.getId());

		int retried = dispatcher.retryFailed();

		assertThat(retried).isEqualTo(1);
		Delivery recovered = delivery(NotificationChannelType.EMAIL);
		assertThat(recovered.getStatus()).isEqualTo(DeliveryStatus.SENT);
		assertThat(recovered.getAttempts()).isEqualTo(2);
		assertThat(email.sent).containsExactly(notification.getId());
		// O push, já entregue, não foi reenviado.
		assertThat(push.sent).hasSize(1);
	}

	@Test
	void paraDeTentarNoLimite() {
		email.failures = 99;
		dispatcher.dispatch(notification.getId());

		assertThat(dispatcher.retryFailed()).isEqualTo(1);
		assertThat(dispatcher.retryFailed()).isEqualTo(1);
		// Terceira falha esgotou as tentativas: nada mais a repetir.
		assertThat(dispatcher.retryFailed()).isZero();

		Delivery exhausted = delivery(NotificationChannelType.EMAIL);
		assertThat(exhausted.getAttempts()).isEqualTo(Delivery.MAX_ATTEMPTS);
		assertThat(exhausted.getStatus()).isEqualTo(DeliveryStatus.FAILED);
		assertThat(email.attempts).isEqualTo(Delivery.MAX_ATTEMPTS);
	}

	@Test
	void repeticaoNaoEnviaParaQuemDesligouOCanalDepois() {
		email.failures = 1;
		dispatcher.dispatch(notification.getId());
		preferences.save(NotificationPreference.of(athleteId, false, true, Instant.now()));

		assertThat(dispatcher.retryFailed()).isZero();

		assertThat(email.sent).isEmpty();
		assertThat(delivery(NotificationChannelType.EMAIL).getStatus()).isEqualTo(DeliveryStatus.FAILED);
	}

	@Test
	void despacharDeNovoNaoReenviaOQueJaFoiEntregue() {
		dispatcher.dispatch(notification.getId());
		dispatcher.dispatch(notification.getId());

		assertThat(email.sent).hasSize(1);
		assertThat(push.sent).hasSize(1);
	}

	@Test
	void avisoInexistenteEIgnorado() {
		dispatcher.dispatch(UUID.randomUUID());

		assertThat(email.attempts).isZero();
		assertThat(push.attempts).isZero();
	}

	// ---- dublês ----

	private static final class FakeChannel implements NotificationChannel {
		private final NotificationChannelType type;
		boolean available = true;
		int failures;
		String skip;
		int attempts;
		final List<UUID> sent = new ArrayList<>();

		FakeChannel(NotificationChannelType type) {
			this.type = type;
		}

		@Override
		public NotificationChannelType type() {
			return type;
		}

		@Override
		public boolean available() {
			return available;
		}

		@Override
		public Result send(Notification notification) {
			attempts++;
			if (failures > 0) {
				failures--;
				throw new RuntimeException("smtp fora do ar");
			}
			if (skip != null) {
				return Result.skipped(skip);
			}
			sent.add(notification.getId());
			return Result.sent();
		}
	}

	private static final class InMemoryNotifications implements NotificationRepository {
		private final Map<UUID, Notification> store = new HashMap<>();

		@Override
		public Notification save(Notification notification) {
			store.put(notification.getId(), notification);
			return notification;
		}

		@Override
		public Optional<Notification> findById(UUID id) {
			return Optional.ofNullable(store.get(id));
		}

		@Override
		public List<Notification> findByAthleteId(UUID athleteId, boolean unreadOnly) {
			return store.values().stream().filter(n -> n.getAthleteId().equals(athleteId)).toList();
		}
	}

	private static final class InMemoryPreferences implements NotificationPreferenceRepository {
		private final Map<UUID, NotificationPreference> store = new HashMap<>();

		@Override
		public Optional<NotificationPreference> findByAthleteId(UUID athleteId) {
			return Optional.ofNullable(store.get(athleteId));
		}

		@Override
		public NotificationPreference save(NotificationPreference preference) {
			store.put(preference.getAthleteId(), preference);
			return preference;
		}
	}

	private static final class InMemoryDeliveries implements DeliveryRepository {
		private final Map<String, Delivery> store = new HashMap<>();

		private static String key(UUID notificationId, NotificationChannelType channel) {
			return notificationId + ":" + channel;
		}

		@Override
		public Delivery save(Delivery delivery) {
			store.put(key(delivery.getNotificationId(), delivery.getChannel()), delivery);
			return delivery;
		}

		@Override
		public Optional<Delivery> find(UUID notificationId, NotificationChannelType channel) {
			return Optional.ofNullable(store.get(key(notificationId, channel)));
		}

		@Override
		public List<Delivery> findRetryable(int maxAttempts) {
			return store.values().stream().filter(d -> d.getStatus() == DeliveryStatus.FAILED
					&& d.getAttempts() < maxAttempts).toList();
		}
	}
}
