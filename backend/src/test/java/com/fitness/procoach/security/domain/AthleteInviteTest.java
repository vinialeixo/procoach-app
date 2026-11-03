package com.fitness.procoach.security.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AthleteInviteTest {

	private static final Instant NOW = Instant.parse("2026-11-03T12:00:00Z");

	private static AthleteInvite invite() {
		return AthleteInvite.create(UUID.randomUUID(), UUID.randomUUID(), "ana@exemplo.com", "hash", NOW,
				NOW.plus(Duration.ofDays(7)));
	}

	@Test
	void conviteNovoEstaPendenteDentroDoPrazo() {
		assertThat(invite().isUsable(NOW.plusSeconds(60))).isTrue();
	}

	@Test
	void conviteExpiradoNaoEhUtilizavel() {
		AthleteInvite invite = invite();

		assertThat(invite.isUsable(NOW.plus(Duration.ofDays(7)))).isFalse();
		assertThat(invite.isUsable(NOW.plus(Duration.ofDays(8)))).isFalse();
	}

	@Test
	void conviteUsadoNaoPodeSerUsadoDeNovo() {
		AthleteInvite invite = invite();
		invite.markUsed(NOW.plusSeconds(60));

		assertThat(invite.isUsable(NOW.plusSeconds(120))).isFalse();
		assertThat(invite.getUsedAt()).isEqualTo(NOW.plusSeconds(60));
		assertThatThrownBy(() -> invite.markUsed(NOW.plusSeconds(120))).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void conviteRevogadoNaoEhUtilizavel() {
		AthleteInvite invite = invite();
		invite.revoke(NOW.plusSeconds(10));

		assertThat(invite.isUsable(NOW.plusSeconds(20))).isFalse();
		assertThatThrownBy(() -> invite.markUsed(NOW.plusSeconds(20))).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void revogarConviteJaUsadoNaoMuda() {
		AthleteInvite invite = invite();
		invite.markUsed(NOW.plusSeconds(10));
		invite.revoke(NOW.plusSeconds(20));

		assertThat(invite.getRevokedAt()).isNull();
	}

	@Test
	void prazoPrecisaEstarNoFuturo() {
		assertThatThrownBy(() -> AthleteInvite.create(UUID.randomUUID(), UUID.randomUUID(), "a@b.com", "hash", NOW, NOW))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void emailEmBrancoEhRecusado() {
		assertThatThrownBy(() -> AthleteInvite.create(UUID.randomUUID(), UUID.randomUUID(), " ", "hash", NOW,
				NOW.plusSeconds(60))).isInstanceOf(IllegalArgumentException.class);
	}
}
