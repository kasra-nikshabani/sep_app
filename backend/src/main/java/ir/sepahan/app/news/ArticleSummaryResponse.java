package ir.sepahan.app.news;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ArticleSummaryResponse(
        UUID id,
        String title,
        String slug,
        String summary,
        String coverImageUrl,
        String categoryName,
        ArticleStatus status,
        OffsetDateTime publishedAt
) {
    public static ArticleSummaryResponse of(Article article) {
        return new ArticleSummaryResponse(article.getId(), article.getTitle(), article.getSlug(), article.getSummary(),
                article.getCoverImageUrl(), article.getCategory().getName(), article.getStatus(), article.getPublishedAt());
    }
}
