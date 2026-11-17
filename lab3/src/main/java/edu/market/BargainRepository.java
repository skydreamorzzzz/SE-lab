package edu.market;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
public interface BargainRepository extends JpaRepository<Bargain, Long> {
    List<Bargain> findByBuyerIdOrSellerIdOrderByIdDesc(Long buyerId, Long sellerId);
    List<Bargain> findByStatusAndExpiresAtBefore(String status, Instant time);
    @Query("select b.productId from Bargain b where b.id = :id")
    Optional<Long> productId(Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Bargain b where b.id = :id")
    Optional<Bargain> locked(Long id);
}
