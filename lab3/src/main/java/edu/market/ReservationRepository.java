package edu.market;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByProductIdAndCancelledFalseAndConfirmedFalseOrderByIdAsc(Long productId);
    List<Reservation> findByBuyerIdOrderByIdDesc(Long buyerId);
    List<Reservation> findByProductIdOrderByIdAsc(Long productId);

    @Query("select r.productId from Reservation r where r.id = :id")
    Optional<Long> productId(Long id);
}
