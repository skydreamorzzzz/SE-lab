package edu.market;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RefundRepository extends JpaRepository<RefundRequest, Long> {
    Optional<RefundRequest> findFirstByOrderIdOrderByIdDesc(Long orderId);
    List<RefundRequest> findByStatusAndCreatedAtBefore(String status, Instant cutoff);
}
