package edu.market;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
public interface PlatformRuleRepository extends JpaRepository<PlatformRule, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from PlatformRule r where r.id = 1")
    Optional<PlatformRule> locked();
}
