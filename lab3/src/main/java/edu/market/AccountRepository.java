package edu.market;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from Account a where a.id = :id")
    Optional<Account> locked(Long id);
    Optional<Account> findByUsername(String username);
    Optional<Account> findByPhone(String phone);
}
