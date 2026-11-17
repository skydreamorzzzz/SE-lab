package edu.market;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    Optional<Settlement> findByOrderId(Long orderId);
    List<Settlement> findBySellerIdOrderByIdDesc(Long sellerId);
}
