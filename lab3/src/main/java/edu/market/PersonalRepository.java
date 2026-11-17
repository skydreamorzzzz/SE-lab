package edu.market;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PersonalRepository extends JpaRepository<PersonalEntry, Long> {
    Optional<PersonalEntry> findByOwnerIdAndKindAndTargetKey(Long ownerId, String kind, String targetKey);
    List<PersonalEntry> findByOwnerIdAndKindOrderByUpdatedAtDesc(Long ownerId, String kind);
    long countByKindAndTargetId(String kind, Long targetId);
}
