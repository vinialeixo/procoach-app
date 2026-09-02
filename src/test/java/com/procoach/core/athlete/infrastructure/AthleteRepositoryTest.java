package com.procoach.core.athlete.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.procoach.core.athlete.domain.Athlete;
import com.procoach.core.athlete.domain.AthleteId;
import com.procoach.core.athlete.domain.AthleteRepository;
import com.procoach.core.athlete.domain.AthleteStatus;
import com.procoach.core.athlete.domain.Goal;
import com.procoach.core.athlete.domain.Height;
import com.procoach.core.athlete.domain.Name;
import com.procoach.core.athlete.domain.Sex;
import com.fitness.procoach.ProcoachApplication;
import com.procoach.core.athlete.domain.Weight;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = ProcoachApplication.class)
@Import(PostgresTestcontainersConfiguration.class)
class AthleteRepositoryTest {

	@Autowired
	private AthleteRepository repository;

	private Athlete newAthlete() {
		return Athlete.register(
				AthleteId.generate(),
				UUID.fromString("22222222-2222-2222-2222-222222222222"),
				new Name("Carlos Pereira"),
				Sex.MALE,
				LocalDate.of(1992, 3, 15),
				new Height(new BigDecimal("178.00")),
				new Weight(new BigDecimal("82.00")),
				Goal.CUTTING);
	}

	@Test
	void savePersisteERecuperaAtleta() {
		Athlete athlete = newAthlete();

		Athlete saved = repository.save(athlete);

		assertThat(saved.id()).isEqualTo(athlete.id());
		assertThat(saved.status()).isEqualTo(AthleteStatus.ACTIVE);

		Optional<Athlete> found = repository.findById(athlete.id());
		assertThat(found).isPresent();
		assertThat(found.get().name().value()).isEqualTo("Carlos Pereira");
		assertThat(found.get().sex()).isEqualTo(Sex.MALE);
		assertThat(found.get().birthDate()).isEqualTo(LocalDate.of(1992, 3, 15));
		assertThat(found.get().height().value()).isEqualByComparingTo("178.00");
		assertThat(found.get().weight().value()).isEqualByComparingTo("82.00");
		assertThat(found.get().goal()).isEqualTo(Goal.CUTTING);
		assertThat(found.get().coachId()).isEqualTo(athlete.coachId());
	}

	@Test
	void findByIdRetornaVazioParaIdInexistente() {
		Optional<Athlete> found = repository.findById(AthleteId.generate());

		assertThat(found).isEmpty();
	}

	@Test
	void existsByIdRefletePresencaDoRegistro() {
		Athlete athlete = newAthlete();
		repository.save(athlete);

		assertThat(repository.existsById(athlete.id())).isTrue();
		assertThat(repository.existsById(AthleteId.generate())).isFalse();
	}

	@Test
	void idNaoEhReutilizadoEntreAtletas() {
		Athlete first = repository.save(newAthlete());
		Athlete second = repository.save(newAthlete());

		assertThat(first.id()).isNotEqualTo(second.id());
		assertThat(repository.existsById(first.id())).isTrue();
		assertThat(repository.existsById(second.id())).isTrue();
	}
}
