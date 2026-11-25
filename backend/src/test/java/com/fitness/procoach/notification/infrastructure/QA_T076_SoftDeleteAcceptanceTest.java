package com.fitness.procoach.notification.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fitness.procoach.audit.application.AuditService;
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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

/**
 * QA independente da T-076 — correção da regra dura de soft-delete: remover token inválido marca
 * {@code deleted_at}, nunca apaga a linha, e registra {@code AuditLog DEVICE_TOKEN_REMOVED}; um
 * cadastro repetido do mesmo token reativa o registro.
 */
class QA_T076_SoftDeleteAcceptanceTest {

	@Test
	void remocaoDeTokenInvalidoMarcaDeletedAtSemApagarFisicamente() {
		DeviceTokenJpaRepository jpa = mock(DeviceTokenJpaRepository.class);
		JpaDeviceTokenRepository repository = new JpaDeviceTokenRepository(jpa);
		DeviceToken device = DeviceToken.register(UUID.randomUUID(), "tok-morto", DevicePlatform.ANDROID,
				Instant.parse("2026-11-25T10:00:00Z"));
		when(jpa.save(any(DeviceTokenJpaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		repository.delete(device);

		ArgumentCaptor<DeviceTokenJpaEntity> saved = ArgumentCaptor.forClass(DeviceTokenJpaEntity.class);
		verify(jpa).save(saved.capture());
		verify(jpa, never()).deleteById(any());
		assertThat(saved.getValue().getId()).isEqualTo(device.getId());
		assertThat(saved.getValue().getDeletedAt()).isNotNull();
		assertThat(device.isDeleted()).isTrue();
	}

	@Test
	void leiturasIgnoramRemovidosMasCadastroRepetidoEnxergaParaReativar() {
		DeviceTokenJpaRepository jpa = mock(DeviceTokenJpaRepository.class);
		JpaDeviceTokenRepository repository = new JpaDeviceTokenRepository(jpa);
		DeviceTokenJpaEntity deleted = new DeviceTokenJpaEntity();
		deleted.setId(UUID.randomUUID());
		deleted.setAthleteId(UUID.randomUUID());
		deleted.setToken("tok-antigo");
		deleted.setPlatform(DevicePlatform.ANDROID);
		deleted.setCreatedAt(Instant.parse("2026-11-01T10:00:00Z"));
		deleted.setLastSeenAt(Instant.parse("2026-11-01T10:00:00Z"));
		deleted.setDeletedAt(Instant.parse("2026-11-20T10:00:00Z"));
		when(jpa.findByToken("tok-antigo")).thenReturn(Optional.of(deleted));
		when(jpa.findByTokenAndDeletedAtIsNull("tok-antigo")).thenReturn(Optional.empty());

		assertThat(repository.findByToken("tok-antigo")).isEmpty();
		assertThat(repository.findByTokenIncludingDeleted("tok-antigo")).get()
			.extracting(DeviceToken::isDeleted)
			.isEqualTo(true);
	}

	@Test
	void remocaoPorTokenInvalidoGeraAuditLogDoDispositivo() {
		UUID athleteId = UUID.randomUUID();
		SoftDeleteDevices devices = new SoftDeleteDevices();
		devices.save(DeviceToken.register(athleteId, "tok-morto", DevicePlatform.IOS, Instant.now()));
		DeviceToken target = devices.store.get(0);
		AuditService audit = mock(AuditService.class);
		PushNotificationChannel channel = new PushNotificationChannel(provider(() -> (token, title, body) -> {
			throw new InvalidPushTokenException(token, "UNREGISTERED");
		}), devices, audit);

		NotificationChannel.Result result = channel.send(Notification.create(athleteId, "TRAINING_CHANGED",
				"Seu treino mudou.", "Training", UUID.randomUUID(), Instant.now()));

		assertThat(result.skipped()).isTrue();
		assertThat(devices.store).hasSize(1);
		assertThat(devices.store.get(0).isDeleted()).isTrue();
		verify(audit).record("DeviceToken", target.getId(), athleteId, "DEVICE_TOKEN_REMOVED", null,
				"{\"reason\":\"invalid_token\"}");
	}

	@Test
	void cadastroRepetidoReativaDispositivoRemovido() {
		UUID athleteId = UUID.randomUUID();
		SoftDeleteDevices devices = new SoftDeleteDevices();
		DeviceToken removed = DeviceToken.register(athleteId, "tok-voltou", DevicePlatform.WEB,
				Instant.parse("2026-11-01T10:00:00Z"));
		removed.remove(Instant.parse("2026-11-10T10:00:00Z"));
		devices.save(removed);

		DeviceToken reloaded = devices.findByTokenIncludingDeleted("tok-voltou").orElseThrow();
		reloaded.seenBy(athleteId, DevicePlatform.WEB, Instant.parse("2026-11-25T10:00:00Z"));
		devices.save(reloaded);

		assertThat(devices.store).hasSize(1);
		assertThat(devices.store.get(0).isDeleted()).isFalse();
		assertThat(devices.findByToken("tok-voltou")).isPresent();
	}

	// ------------------------------------------------------------------
	// infraestrutura de teste
	// ------------------------------------------------------------------

	private static ObjectProvider<PushGateway> provider(Supplier<PushGateway> supplier) {
		return new ObjectProvider<>() {
			@Override
			public PushGateway getObject(Object... args) {
				return supplier.get();
			}

			@Override
			public PushGateway getObject() {
				return supplier.get();
			}

			@Override
			public PushGateway getIfAvailable() {
				return supplier.get();
			}

			@Override
			public PushGateway getIfUnique() {
				return supplier.get();
			}
		};
	}

	/** Espelha a semântica do adapter JPA: remoção marca, leituras normais filtram. */
	private static final class SoftDeleteDevices implements DeviceTokenRepository {

		final List<DeviceToken> store = new ArrayList<>();

		@Override
		public DeviceToken save(DeviceToken device) {
			store.removeIf(existing -> existing.getId().equals(device.getId()));
			store.add(device);
			return device;
		}

		@Override
		public Optional<DeviceToken> findByToken(String token) {
			return store.stream().filter(device -> device.getToken().equals(token) && !device.isDeleted()).findFirst();
		}

		@Override
		public Optional<DeviceToken> findByTokenIncludingDeleted(String token) {
			return store.stream().filter(device -> device.getToken().equals(token)).findFirst();
		}

		@Override
		public List<DeviceToken> findByAthleteId(UUID athleteId) {
			return store.stream()
				.filter(device -> device.getAthleteId().equals(athleteId) && !device.isDeleted())
				.toList();
		}

		@Override
		public void delete(DeviceToken device) {
			device.remove(Instant.now());
			save(device);
		}

	}

}
