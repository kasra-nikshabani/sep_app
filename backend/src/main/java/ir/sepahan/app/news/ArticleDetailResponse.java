package ir.sepahan.app.news;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ArticleDetailResponse(
        UUID id,
        String title,
        String slug,
        String summary,
        String content,
        String coverImageUrl,
        String metaTitle,
        String metaDescription,
        String categoryName,
        String authorDisplayName,
        List<TagResponse> tags,
        ArticleStatus status,
        OffsetDateTime publishedAt,
        OffsetDateTime scheduledAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ArticleDetailResponse of(Article article, String authorDisplayName) {
        return new ArticleDetailResponse(
                article.getId(), article.getTitle(), article.getSlug(), article.getSummary(), article.getContent(),
                article.getCoverImageUrl(), article.getMetaTitle(), article.getMetaDescription(),
                article.getCategory().getName(), authorDisplayName,
                article.getTags().stream().map(TagResponse::of).toList(),
                article.getStatus(), article.getPublishedAt(), article.getScheduledAt(),
                article.getCreatedAt(), article.getUpdatedAt());
    }
}
