package ir.sepahan.app.news;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleRepository extends JpaRepository<Article, UUID> {

    Optional<Article> findBySlugAndStatusAndDeletedAtIsNull(String slug, ArticleStatus status);

    Optional<Article> findByIdAndDeletedAtIsNull(UUID id);

    List<Article> findByStatusAndDeletedAtIsNullOrderByPublishedAtDesc(ArticleStatus status);

    List<Article> findByStatusAndCategoryIdAndDeletedAtIsNullOrderByPublishedAtDesc(ArticleStatus status, UUID categoryId);

    List<Article> findByDeletedAtIsNullOrderByCreatedAtDesc();

    List<Article> findByStatusAndScheduledAtBefore(ArticleStatus status, OffsetDateTime cutoff);

    // نسخه‌های صفحه‌بندی‌شده (مرتب‌سازی از Pageable) -- با ورود خودکار هزاران خبر، لیست کامل دیگر ممکن نیست.
    List<Article> findByStatusAndDeletedAtIsNull(ArticleStatus status, Pageable pageable);

    List<Article> findByStatusAndCategoryIdAndDeletedAtIsNull(ArticleStatus status, UUID categoryId, Pageable pageable);

    @Query("select a.sourceId from Article a where a.sourceId in :ids")
    List<Long> findExistingSourceIds(@Param("ids") Collection<Long> ids);
}
