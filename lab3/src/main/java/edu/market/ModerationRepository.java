package edu.market;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ModerationRepository extends JpaRepository<ModerationCase, Long> {
    boolean existsByAuthorIdAndTargetTypeAndTargetIdAndReportDateAndKind(Long authorId, String targetType, Long targetId, LocalDate day, String kind);
    boolean existsByAuthorIdAndTargetTypeAndTargetIdAndKindAndStatus(Long authorId, String targetType, Long targetId, String kind, String status);
    List<ModerationCase> findByAuthorIdOrderByIdDesc(Long authorId);
    List<ModerationCase> findAllByOrderByIdDesc();
    interface TargetProjection {
        String getTargetType();
        Long getTargetId();
    }
    @org.springframework.data.jpa.repository.Query("select m.targetType as targetType, m.targetId as targetId from ModerationCase m where m.id = :id")
    java.util.Optional<TargetProjection> target(Long id);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select m from ModerationCase m where m.id = :id")
    java.util.Optional<ModerationCase> locked(Long id);
}
