package com.vigilai.repository;

import com.vigilai.entity.AccountabilityStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountabilityStatsRepository extends JpaRepository<AccountabilityStats, Long> {
    Optional<AccountabilityStats> findByUserId(Long userId);
    List<AccountabilityStats> findByUserIdIn(List<Long> userIds);
}
