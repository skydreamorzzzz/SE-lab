package edu.market;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CommentRepository extends JpaRepository<ProductComment, Long> {
    List<ProductComment> findByProductIdOrderByIdDesc(Long productId);
}
