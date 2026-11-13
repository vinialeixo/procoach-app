package com.fitness.procoach.audit.application;

import java.util.Optional;
import java.util.UUID;

/**
 * Fornece informações legíveis do autor (nome e e-mail) para enriquecer a auditoria.
 * Implementado pela camada de infraestrutura (security) para não acoplar o módulo de auditoria.
 */
public interface ActorInfoProvider {

    /**
     * Retorna o nome do ator (coach ou atleta) dado o ID do usuário.
     */
    Optional<String> actorName(UUID userId);

    /**
     * Retorna o e-mail do ator (coach ou atleta) dado o ID do usuário.
     */
    Optional<String> actorEmail(UUID userId);
}