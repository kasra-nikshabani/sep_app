package ir.sepahan.app.loyalty;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoyaltyLevelRepository extends JpaRepository<LoyaltyLevel, UUID> {

    List<LoyaltyLevel> findByDeletedAtIsNullOrderByMinPointsAsc();

    Optional<LoyaltyLevel> findTopByMinPointsLessThanEqualAndDeletedAtIsNullOrderByMinPointsDesc(int lifetimePoints);
}
