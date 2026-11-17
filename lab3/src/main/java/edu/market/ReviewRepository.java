package edu.market;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByOrderIdAndAuthorId(Long orderId, Long authorId);
    List<Review> findByProductIdOrderByIdDesc(Long productId);
    List<Review> findByRecipientIdOrderByIdDesc(Long recipientId);
}
