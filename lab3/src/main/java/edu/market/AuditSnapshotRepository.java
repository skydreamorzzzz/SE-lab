package edu.market;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AuditSnapshotRepository extends JpaRepository<AuditSnapshot, Long> {
    List<AuditSnapshot> findAllByOrderByIdDesc();
}
