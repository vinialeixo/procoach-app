package com.fitness.procoach.athlete.interfaces;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;

/**
 * Resumo unificado dos protocolos de um atleta (T-020/US-004).
 * Agrega TrainingPlans, Diets e Assessments indicando quais estão ativos.
 */
public record ProtocolSummaryResponse(
        UUID athleteId,
        List<TrainingPlanSummary> trainingPlans,
        List<DietSummary> diets,
        List<AssessmentSummary> assessments
) {

    public record TrainingPlanSummary(
            UUID id,
            String name,
            String status,
            boolean active,
            UUID periodizationId,
            int version
    ) {}

    public record DietSummary(
            UUID id,
            String objective,
            String status,
            boolean active,
            int version
    ) {}

    public record AssessmentSummary(
            UUID id,
            String assessmentDate,
            boolean latest
    ) {}
}