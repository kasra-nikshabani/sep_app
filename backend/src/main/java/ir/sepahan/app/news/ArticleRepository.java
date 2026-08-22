package ir.sepahan.app.news;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepository extends JpaRepository<Article, UUID> {

    Optional<Article> findBySlugAndStatusAndDeletedAtIsNull(String slug, ArticleStatus status);

    Optional<Article> findByIdAndDeletedAtIsNull(UUID id);

    List<Article> findByStatusAndDeletedAtIsNullOrderByPublishedAtDesc(ArticleStatus status);

    List<Article> findByStatusAndCategoryIdAndDeletedAtIsNullOrderByPublishedAtDesc(ArticleStatus status, UUID categoryId);

    List<Article> findByDeletedAtIsNullOrderByCreatedAtDesc();

    List<Article> findByStatusAndScheduledAtBefore(ArticleStatus status, OffsetDateTime cutoff);
}
