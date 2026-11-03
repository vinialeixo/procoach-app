package com.fitness.procoach.security.infrastructure;

import com.fitness.procoach.security.domain.AthleteInvite;
import com.fitness.procoach.security.domain.AthleteInviteRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaAthleteInviteRepository implements AthleteInviteRepository {

	private final AthleteInviteJpaRepository jpaRepository;

	public JpaAthleteInviteRepository(AthleteInviteJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	@Transactional
	public AthleteInvite save(AthleteInvite invite) {
		return toDomain(jpaRepository.save(toEntity(invite)));
	}

	@Override
	@Transactional
	public Optional<AthleteInvite> findByTokenHashForUpdate(String tokenHash) {
		return jpaRepository.findByTokenHashForUpdate(tokenHash).map(JpaAthleteInviteRepository::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<AthleteInvite> findByTokenHash(String tokenHash) {
		return jpaRepository.findByTokenHash(tokenHash).map(JpaAthleteInviteRepository::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public List<AthleteInvite> findOpenByAthleteId(UUID athleteId) {
		return jpaRepository.findByAthleteIdAndUsedAtIsNullAndRevokedAtIsNull(athleteId).stream()
				.map(JpaAthleteInviteRepository::toDomain)
				.toList();
	}

	private static AthleteInviteJpaEntity toEntity(AthleteInvite invite) {
		AthleteInviteJpaEntity entity = new AthleteInviteJpaEntity();
		entity.setId(invite.getId());
		entity.setAthleteId(invite.getAthleteId());
		entity.setCoachId(invite.getCoachId());
		entity.setEmail(invite.getEmail());
		entity.setTokenHash(invite.getTokenHash());
		entity.setExpiresAt(invite.getExpiresAt());
		entity.setUsedAt(invite.getUsedAt());
		entity.setRevokedAt(invite.getRevokedAt());
		entity.setCreatedAt(invite.getCreatedAt());
		return entity;
	}

	private static AthleteInvite toDomain(AthleteInviteJpaEntity entity) {
		return AthleteInvite.reconstitute(entity.getId(), entity.getAthleteId(), entity.getCoachId(),
				entity.getEmail(), entity.getTokenHash(), entity.getExpiresAt(), entity.getUsedAt(),
				entity.getRevokedAt(), entity.getCreatedAt());
	}
}
