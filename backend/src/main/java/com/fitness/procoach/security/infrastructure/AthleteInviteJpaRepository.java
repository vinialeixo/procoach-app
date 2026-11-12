package com.fitness.procoach.security.infrastructure;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AthleteInviteJpaRepository extends JpaRepository<AthleteInviteJpaEntity, UUID> {

	Optional<AthleteInviteJpaEntity> findByTokenHash(String tokenHash);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select i from AthleteInviteJpaEntity i where i.tokenHash = :tokenHash")
	Optional<AthleteInviteJpaEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

	List<AthleteInviteJpaEntity> findByAthleteIdAndUsedAtIsNullAndRevokedAtIsNull(UUID athleteId);

	List<AthleteInviteJpaEntity> findByAthleteId(UUID athleteId);
}
