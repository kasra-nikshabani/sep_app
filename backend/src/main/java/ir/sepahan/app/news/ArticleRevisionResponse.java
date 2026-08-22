package ir.sepahan.app.news;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ArticleRevisionResponse(UUID id, String title, String summary, String content, UUID editedBy, OffsetDateTime editedAt) {
    public static ArticleRevisionResponse of(ArticleRevision revision) {
        return new ArticleRevisionResponse(revision.getId(), revision.getTitle(), revision.getSummary(),
                revision.getContent(), revision.getEditedBy(), revision.getEditedAt());
    }
}
