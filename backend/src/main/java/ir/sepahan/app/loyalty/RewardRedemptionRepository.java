package ir.sepahan.app.loyalty;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardRedemptionRepository extends JpaRepository<RewardRedemption, UUID> {

    List<RewardRedemption> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    List<RewardRedemption> findByOrderByCreatedAtDesc();
}
