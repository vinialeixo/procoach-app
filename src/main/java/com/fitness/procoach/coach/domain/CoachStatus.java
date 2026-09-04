package com.fitness.procoach.coach.domain;

/**
 * Estados do ciclo de vida do Coach (UML 4.1.2): ACTIVE pode gerenciar a
 * carteira; SUSPENDED é reversível (volta a ACTIVE); CANCELLED é terminal.
 */
public enum CoachStatus {
	ACTIVE, SUSPENDED, CANCELLED
}
