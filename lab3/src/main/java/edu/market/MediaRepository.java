package edu.market;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MediaRepository extends JpaRepository<MediaAsset, Long> {
    Optional<MediaAsset> findByStoredName(String storedName);
}
