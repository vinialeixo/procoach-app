package com.fitness.procoach.nutrition.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Entidade JPA para suplementos da dieta (T-090).
 * Mapeada para a tabela {@code diet_supplements} a ser criada por migração Flyway.
 */
@Entity
@Table(name = "diet_supplements")
public class SupplementJpaEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diet_id", nullable = false)
    private DietJpaEntity diet;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String dose;

    @Column
    private String timing;

    @Column(name = "counts_toward_macros", nullable = false)
    private boolean countsTowardMacros;

    protected SupplementJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public DietJpaEntity getDiet() {
        return diet;
    }

    public void setDiet(DietJpaEntity diet) {
        this.diet = diet;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDose() {
        return dose;
    }

    public void setDose(String dose) {
        this.dose = dose;
    }

    public String getTiming() {
        return timing;
    }

    public void setTiming(String timing) {
        this.timing = timing;
    }

    public boolean isCountsTowardMacros() {
        return countsTowardMacros;
    }

    public void setCountsTowardMacros(boolean countsTowardMacros) {
        this.countsTowardMacros = countsTowardMacros;
    }
}