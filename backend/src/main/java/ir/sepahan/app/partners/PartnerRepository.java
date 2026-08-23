package ir.sepahan.app.partners;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartnerRepository extends JpaRepository<Partner, UUID> {

    List<Partner> findByDeletedAtIsNull();

    Optional<Partner> findByApiKeyHashAndActiveTrueAndDeletedAtIsNull(String apiKeyHash);
}
