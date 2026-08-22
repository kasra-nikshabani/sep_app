package ir.sepahan.app.loyalty;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, UUID> {

    Optional<LoyaltyTransaction> findBySourceTypeAndSourceReferenceId(String sourceType, UUID sourceReferenceId);

    List<LoyaltyTransaction> findByAccountIdOrderByCreatedAtDesc(UUID accountId);
}
