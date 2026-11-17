package edu.market;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findByBuyerIdOrSellerIdOrderByIdDesc(Long buyerId, Long sellerId);
    List<PurchaseOrder> findByStatusAndCreatedAtBefore(OrderStatus status, Instant time);
    List<PurchaseOrder> findByStatusAndSignedAtBefore(OrderStatus status, Instant time);
    Optional<PurchaseOrder> findByRequestKey(String requestKey);

    @Query("select o.productId from PurchaseOrder o where o.id = :id")
    Optional<Long> productId(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from PurchaseOrder o where o.id = :id")
    Optional<PurchaseOrder> lockById(Long id);
}
