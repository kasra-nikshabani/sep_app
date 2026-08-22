package ir.sepahan.app.news;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsCategoryRepository extends JpaRepository<NewsCategory, UUID> {

    List<NewsCategory> findByDeletedAtIsNull();
}
