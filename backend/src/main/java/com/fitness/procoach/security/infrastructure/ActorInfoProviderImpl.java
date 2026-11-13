package com.fitness.procoach.security.infrastructure;

import com.fitness.procoach.audit.application.ActorInfoProvider;
import com.fitness.procoach.coach.domain.CoachRepository;
import com.fitness.procoach.coach.domain.CoachId;
import com.fitness.procoach.security.domain.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Implementação do ActorInfoProvider que busca nome e e-mail do usuário
 * e, se for coach, também o nome do coach.
 */
@Component
public class ActorInfoProviderImpl implements ActorInfoProvider {

    private final UserRepository users;
    private final CoachRepository coaches;

    public ActorInfoProviderImpl(UserRepository users, CoachRepository coaches) {
        this.users = users;
        this.coaches = coaches;
    }

    @Override
    public Optional<String> actorName(UUID userId) {
        // Tenta buscar o nome do coach primeiro (já que o coach é o autor das ações de dieta/treino/avaliação)
        return coaches.findByUserId(userId)
                .map(coach -> coach.name())
                .or(() -> users.findById(userId).map(user -> user.getUsername()));
    }

    @Override
    public Optional<String> actorEmail(UUID userId) {
        return users.findById(userId)
                .filter(user -> user.isEnabled())
                .map(user -> user.getEmail());
    }
}