package ir.sepahan.app.shop;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findByActiveTrueAndDeletedAtIsNull();

    List<Product> findByCategoryIdAndActiveTrueAndDeletedAtIsNull(UUID categoryId);

    Optional<Product> findByIdAndDeletedAtIsNull(UUID id);
}
