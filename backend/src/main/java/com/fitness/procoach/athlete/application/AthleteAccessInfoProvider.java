package com.fitness.procoach.athlete.application;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta para o estado de acesso do atleta (T-077/T-078): o módulo athlete
 * consulta, sem depender de security, se o atleta já tem credencial de acesso
 * (User), se há convite pendente e se um e-mail já pertence a um User. A
 * implementação vive no módulo security (evita ciclo de dependência no
 * Modulith) — mesmo padrão de {@code AthleteContactProvider}.
 */
public interface AthleteAccessInfoProvider {

	/** O atleta já criou a própria credencial (User vinculado)? */
	boolean hasCredential(UUID athleteId);

	/** Convite ativo (não usado, não revogado e dentro do prazo), se houver. */
	Optional<PendingInvite> pendingInvite(UUID athleteId);

	/** O e-mail já pertence a algum User (coach ou atleta)? */
	boolean emailBelongsToUser(String email);

	/**
	 * Revoga o acesso do atleta (desabilita User e revoga convites pendentes)
	 * após soft-delete ou transição para {@code FINISHED}.
	 */
	void revokeAccess(UUID athleteId, UUID actorUserId);

	/**
	 * Reabilita a credencial do atleta quando ele retorna de {@code FINISHED}
	 * para um estado ativo/pausado.
	 */
	void restoreAccess(UUID athleteId);

	/** Visão mínima do convite pendente — nunca expõe o token. */
	record PendingInvite(UUID id, String email, Instant expiresAt) {
	}
}
