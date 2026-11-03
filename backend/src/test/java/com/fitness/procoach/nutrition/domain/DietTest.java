package com.fitness.procoach.nutrition.domain;

import com.fitness.procoach.coach.domain.CoachId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DietTest {

    private static final CoachId COACH = CoachId.of(UUID.randomUUID());
    private static final AthleteId ATHLETE = AthleteId.of(UUID.randomUUID());
    private static final UUID AUTHOR = UUID.randomUUID();
    private static final Macronutrients MACROS = Macronutrients.of(2500, 180, 250, 80);

    @Test
    void create_shouldBuildFirstVersionAsDraft() {
        Diet diet = Diet.create(COACH, ATHLETE, DietObjective.HYPERTROPHY, MACROS, AUTHOR);

        assertThat(diet.version()).isEqualTo(1);
        assertThat(diet.status()).isEqualTo(DietStatus.DRAFT);
        assertThat(diet.objective()).isEqualTo(DietObjective.HYPERTROPHY);
        assertThat(diet.macros()).isEqualTo(MACROS);
        assertThat(diet.coachId()).isEqualTo(COACH);
        assertThat(diet.athleteId()).isEqualTo(ATHLETE);
        assertThat(diet.previousVersionId()).isNull();
        assertThat(diet.isReadOnly()).isFalse();
        assertThat(diet.isDeleted()).isFalse();
        assertThat(diet.createdBy()).isEqualTo(AUTHOR);
    }

    @Test
    void create_shouldEmitDietCreatedEvent() {
        Diet diet = Diet.create(COACH, ATHLETE, DietObjective.CUTTING, MACROS, AUTHOR);

        assertThat(diet.domainEvents()).hasSize(1);
        assertThat(diet.domainEvents().get(0)).isInstanceOf(DietCreated.class);
    }

    @Test
    void createNewVersion_shouldChainHistoryAndFreezePreviousVersion() {
        Diet v1 = Diet.create(COACH, ATHLETE, DietObjective.CUTTING, MACROS, AUTHOR);

        Macronutrients newMacros = Macronutrients.of(2200, 200, 200, 60);
        Diet v2 = v1.createNewVersion(DietObjective.CUTTING, newMacros, "Redução de calorias", AUTHOR);

        assertThat(v2.version()).isEqualTo(2);
        assertThat(v2.previousVersionId()).isEqualTo(v1.id());
        assertThat(v2.macros()).isEqualTo(newMacros);
        assertThat(v2.status()).isEqualTo(DietStatus.DRAFT);
        assertThat(v2.changeReason()).isEqualTo("Redução de calorias");
        assertThat(v1.isReadOnly()).isTrue();
        assertThat(v1.domainEvents()).hasSize(1);
        assertThat(v1.domainEvents().get(0)).isInstanceOf(DietCreated.class);
        assertThat(v2.domainEvents()).hasSize(1);
        assertThat(v2.domainEvents().get(0)).isInstanceOf(DietVersioned.class);
    }

    @Test
    void createNewVersion_shouldRequireChangeReason() {
        Diet v1 = Diet.create(COACH, ATHLETE, DietObjective.CUTTING, MACROS, AUTHOR);

        assertThatThrownBy(() -> v1.createNewVersion(DietObjective.CUTTING, MACROS, "  ", AUTHOR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("changeReason");
    }

    @Test
    void createNewVersion_shouldRejectFromReadOnlyVersion() {
        Diet v1 = Diet.create(COACH, ATHLETE, DietObjective.CUTTING, MACROS, AUTHOR);
        v1.createNewVersion(DietObjective.CUTTING, MACROS, "Ajuste", AUTHOR);

        assertThatThrownBy(() -> v1.createNewVersion(DietObjective.CUTTING, MACROS, "Outro", AUTHOR))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("read-only");
    }

    @Test
    void createNewVersion_shouldRejectFromDeletedDiet() {
        Diet v1 = Diet.create(COACH, ATHLETE, DietObjective.CUTTING, MACROS, AUTHOR);
        v1.softDelete(AUTHOR);

        assertThatThrownBy(() -> v1.createNewVersion(DietObjective.CUTTING, MACROS, "Ajuste", AUTHOR))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deleted");
    }

    @Test
    void changeStatus_shouldFollowApprovalFlow() {
        Diet diet = Diet.create(COACH, ATHLETE, DietObjective.CUTTING, MACROS, AUTHOR);

        diet.changeStatus(DietStatus.IN_REVIEW);
        assertThat(diet.status()).isEqualTo(DietStatus.IN_REVIEW);

        diet.changeStatus(DietStatus.APPROVED);
        assertThat(diet.status()).isEqualTo(DietStatus.APPROVED);

        diet.changeStatus(DietStatus.ARCHIVED);
        assertThat(diet.status()).isEqualTo(DietStatus.ARCHIVED);
    }

    @Test
    void changeStatus_shouldEmitDietStatusChangedEvent() {
        Diet diet = Diet.create(COACH, ATHLETE, DietObjective.CUTTING, MACROS, AUTHOR);

        diet.changeStatus(DietStatus.IN_REVIEW);

        assertThat(diet.domainEvents()).hasSize(2);
        assertThat(diet.domainEvents().get(1)).isInstanceOf(DietStatusChanged.class);
    }

    @Test
    void changeStatus_shouldBlockInvalidTransitions() {
        Diet diet = Diet.create(COACH, ATHLETE, DietObjective.CUTTING, MACROS, AUTHOR);

        assertThatThrownBy(() -> diet.changeStatus(DietStatus.APPROVED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    void changeStatus_shouldRejectOnReadOnlyVersion() {
        Diet v1 = Diet.create(COACH, ATHLETE, DietObjective.CUTTING, MACROS, AUTHOR);
        v1.createNewVersion(DietObjective.CUTTING, MACROS, "Ajuste", AUTHOR);

        assertThatThrownBy(() -> v1.changeStatus(DietStatus.IN_REVIEW))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("read-only");
    }

    @Test
    void softDelete_shouldMarkAsDeletedAndEmitEvent() {
        Diet diet = Diet.create(COACH, ATHLETE, DietObjective.CUTTING, MACROS, AUTHOR);

        diet.softDelete(AUTHOR);

        assertThat(diet.isDeleted()).isTrue();
        assertThat(diet.domainEvents()).hasSize(2);
        assertThat(diet.domainEvents().get(1)).isInstanceOf(DietDeleted.class);
    }

    @Test
    void softDelete_shouldBeIdempotent() {
        Diet diet = Diet.create(COACH, ATHLETE, DietObjective.CUTTING, MACROS, AUTHOR);

        diet.softDelete(AUTHOR);
        diet.softDelete(AUTHOR);

        assertThat(diet.isDeleted()).isTrue();
        assertThat(diet.domainEvents()).hasSize(2);
    }

    @Test
    void macronutrients_shouldRejectNonPositiveValues() {
        assertThatThrownBy(() -> Macronutrients.of(0, 180, 250, 80))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("calories");
        assertThatThrownBy(() -> Macronutrients.of(2500, -1, 250, 80))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("protein");
        assertThatThrownBy(() -> Macronutrients.of(2500, 180, 0, 80))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("carbohydrates");
        assertThatThrownBy(() -> Macronutrients.of(2500, 180, 250, -5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fat");
    }

    @Test
    void clearDomainEvents_shouldEmptyTheList() {
        Diet diet = Diet.create(COACH, ATHLETE, DietObjective.CUTTING, MACROS, AUTHOR);

        diet.clearDomainEvents();

        assertThat(diet.domainEvents()).isEmpty();
    }
}
