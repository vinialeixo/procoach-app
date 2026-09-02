package com.fitness.procoach.training.domain.model;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PeriodizationTest {

	@Test
	void createsPeriodizationWithDates() {
		LocalDate start = LocalDate.of(2026, 9, 1);
		LocalDate end = LocalDate.of(2026, 12, 1);

		Periodization p = Periodization.create(UUID.randomUUID(), UUID.randomUUID(), "Macrociclo", "12 semanas",
				start, end);

		assertThat(p.getId()).isNotNull();
		assertThat(p.getStartDate()).isEqualTo(start);
		assertThat(p.getEndDate()).isEqualTo(end);
		assertThat(p.getStatus()).isEqualTo(PeriodizationStatus.DRAFT);
		assertThat(p.isDeleted()).isFalse();
	}

	@Test
	void rejectsEndDateBeforeStartDate() {
		assertThatThrownBy(() -> Periodization.create(UUID.randomUUID(), UUID.randomUUID(), "P", null,
				LocalDate.of(2026, 12, 1), LocalDate.of(2026, 9, 1)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("endDate must not be before startDate");
	}

	@Test
	void rejectsNullStartDate() {
		assertThatThrownBy(() -> Periodization.create(UUID.randomUUID(), UUID.randomUUID(), "P", null, null,
				LocalDate.of(2026, 12, 1)))
			.isInstanceOf(NullPointerException.class);
	}

	@Test
	void rejectsNullEndDate() {
		assertThatThrownBy(() -> Periodization.create(UUID.randomUUID(), UUID.randomUUID(), "P", null,
				LocalDate.of(2026, 9, 1), null))
			.isInstanceOf(NullPointerException.class);
	}

	@Test
	void activatesFromDraft() {
		Periodization p = Periodization.create(UUID.randomUUID(), UUID.randomUUID(), "P", null,
				LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 1));

		p.activate();

		assertThat(p.getStatus()).isEqualTo(PeriodizationStatus.ACTIVE);
	}

	@Test
	void finishesFromActive() {
		Periodization p = Periodization.create(UUID.randomUUID(), UUID.randomUUID(), "P", null,
				LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 1));
		p.activate();

		p.finish();

		assertThat(p.getStatus()).isEqualTo(PeriodizationStatus.FINISHED);
	}

	@Test
	void rejectsActivateWhenNotDraft() {
		Periodization p = Periodization.create(UUID.randomUUID(), UUID.randomUUID(), "P", null,
				LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 1));
		p.activate();

		assertThatThrownBy(p::activate)
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Only DRAFT periodizations can be activated");
	}

	@Test
	void rejectsFinishWhenNotActive() {
		Periodization p = Periodization.create(UUID.randomUUID(), UUID.randomUUID(), "P", null,
				LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 1));

		assertThatThrownBy(p::finish)
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Only ACTIVE periodizations can be finished");
	}

	@Test
	void archivesFromAnyStatus() {
		Periodization p = Periodization.create(UUID.randomUUID(), UUID.randomUUID(), "P", null,
				LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 1));

		p.archive();

		assertThat(p.getStatus()).isEqualTo(PeriodizationStatus.ARCHIVED);
	}

	@Test
	void rejectsArchiveWhenAlreadyArchived() {
		Periodization p = Periodization.create(UUID.randomUUID(), UUID.randomUUID(), "P", null,
				LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 1));
		p.archive();

		assertThatThrownBy(p::archive)
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("already archived");
	}

	@Test
	void softDeletesPeriodization() {
		Periodization p = Periodization.create(UUID.randomUUID(), UUID.randomUUID(), "P", null,
				LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 1));

		p.softDelete();

		assertThat(p.isDeleted()).isTrue();
		assertThat(p.getDeletedAt()).isNotNull();
	}

	@Test
	void rejectsOperationOnSoftDeletedPeriodization() {
		Periodization p = Periodization.create(UUID.randomUUID(), UUID.randomUUID(), "P", null,
				LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 1));
		p.softDelete();

		assertThatThrownBy(p::activate)
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("soft-deleted");
	}

}
