package com.procoach.core.athlete.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AthleteTest {

	private static final UUID COACH_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

	private Athlete newAthlete() {
		return Athlete.register(
				AthleteId.generate(),
				COACH_ID,
				new Name("João Silva"),
				Sex.MALE,
				LocalDate.of(1995, 5, 20),
				new Height(new BigDecimal("180.00")),
				new Weight(new BigDecimal("85.50")),
				Goal.HYPERTROPHY);
	}

	@Test
	void registerCriaAtletaComIdUnicoEImutavel() {
		Athlete athlete = newAthlete();

		assertThat(athlete.id()).isNotNull();
		assertThat(athlete.id().value()).isNotNull();
		assertThat(athlete.status()).isEqualTo(AthleteStatus.ACTIVE);
		assertThat(athlete.createdAt()).isNotNull();
		assertThat(athlete.updatedAt()).isNotNull();
	}

	@Test
	void idsGeradosSaoDiferentes() {
		AthleteId first = AthleteId.generate();
		AthleteId second = AthleteId.generate();

		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void idNaoPodeSerAlteradoAposCriacao() {
		Athlete athlete = newAthlete();
		AthleteId original = athlete.id();

		// O id é final e exposto apenas via getter; não há setter.
		assertThat(athlete.id()).isEqualTo(original);
	}

	@Test
	void registerValidaCamposObrigatorios() {
		assertThatThrownBy(() -> Athlete.register(null, COACH_ID, new Name("A"), Sex.MALE,
				LocalDate.of(1995, 5, 20), new Height(BigDecimal.ONE), new Weight(BigDecimal.ONE), Goal.CUTTING))
				.isInstanceOf(NullPointerException.class);

		assertThatThrownBy(() -> Athlete.register(AthleteId.generate(), null, new Name("A"), Sex.MALE,
				LocalDate.of(1995, 5, 20), new Height(BigDecimal.ONE), new Weight(BigDecimal.ONE), Goal.CUTTING))
				.isInstanceOf(NullPointerException.class);

		assertThatThrownBy(() -> Athlete.register(AthleteId.generate(), COACH_ID, null, Sex.MALE,
				LocalDate.of(1995, 5, 20), new Height(BigDecimal.ONE), new Weight(BigDecimal.ONE), Goal.CUTTING))
				.isInstanceOf(NullPointerException.class);

		assertThatThrownBy(() -> Athlete.register(AthleteId.generate(), COACH_ID, new Name("A"), null,
				LocalDate.of(1995, 5, 20), new Height(BigDecimal.ONE), new Weight(BigDecimal.ONE), Goal.CUTTING))
				.isInstanceOf(NullPointerException.class);

		assertThatThrownBy(() -> Athlete.register(AthleteId.generate(), COACH_ID, new Name("A"), Sex.MALE,
				null, new Height(BigDecimal.ONE), new Weight(BigDecimal.ONE), Goal.CUTTING))
				.isInstanceOf(NullPointerException.class);

		assertThatThrownBy(() -> Athlete.register(AthleteId.generate(), COACH_ID, new Name("A"), Sex.MALE,
				LocalDate.of(1995, 5, 20), null, new Weight(BigDecimal.ONE), Goal.CUTTING))
				.isInstanceOf(NullPointerException.class);

		assertThatThrownBy(() -> Athlete.register(AthleteId.generate(), COACH_ID, new Name("A"), Sex.MALE,
				LocalDate.of(1995, 5, 20), new Height(BigDecimal.ONE), null, Goal.CUTTING))
				.isInstanceOf(NullPointerException.class);

		assertThatThrownBy(() -> Athlete.register(AthleteId.generate(), COACH_ID, new Name("A"), Sex.MALE,
				LocalDate.of(1995, 5, 20), new Height(BigDecimal.ONE), new Weight(BigDecimal.ONE), null))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	void registerRejeitaDataDeNascimentoFutura() {
		assertThatThrownBy(() -> Athlete.register(AthleteId.generate(), COACH_ID, new Name("A"), Sex.MALE,
				LocalDate.now().plusDays(1), new Height(BigDecimal.ONE), new Weight(BigDecimal.ONE), Goal.CUTTING))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("future");
	}

	@Test
	void nameRejeitaValorVazio() {
		assertThatThrownBy(() -> new Name("   ")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new Name(null)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void heightEWeightRejeitamValoresNaoPositivos() {
		assertThatThrownBy(() -> new Height(BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new Height(new BigDecimal("-1"))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new Weight(BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new Weight(new BigDecimal("-1"))).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void reconstitutePreservaEstadoPersistido() {
		AthleteId id = AthleteId.generate();
		LocalDate birthDate = LocalDate.of(1990, 1, 1);
		Athlete athlete = Athlete.reconstitute(
				id, COACH_ID, new Name("Maria"), Sex.FEMALE, birthDate,
				new Height(new BigDecimal("165.00")), new Weight(new BigDecimal("60.00")),
				Goal.MAINTENANCE, AthleteStatus.PAUSED,
				java.time.Instant.parse("2026-01-01T10:00:00Z"),
				java.time.Instant.parse("2026-01-02T10:00:00Z"));

		assertThat(athlete.id()).isEqualTo(id);
		assertThat(athlete.coachId()).isEqualTo(COACH_ID);
		assertThat(athlete.name().value()).isEqualTo("Maria");
		assertThat(athlete.sex()).isEqualTo(Sex.FEMALE);
		assertThat(athlete.birthDate()).isEqualTo(birthDate);
		assertThat(athlete.height().value()).isEqualByComparingTo("165.00");
		assertThat(athlete.weight().value()).isEqualByComparingTo("60.00");
		assertThat(athlete.goal()).isEqualTo(Goal.MAINTENANCE);
		assertThat(athlete.status()).isEqualTo(AthleteStatus.PAUSED);
	}
}
