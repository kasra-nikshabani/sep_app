package ir.sepahan.app.news;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;

public record CreateArticleRequest(
        @NotNull UUID categoryId,
        @NotBlank String title,
        @NotBlank String slug,
        @NotBlank String content,
        String summary,
        String coverImageUrl,
        String metaTitle,
        String metaDescription,
        Set<UUID> tagIds
) {
    public ArticleWriteCommand toCommand() {
        return new ArticleWriteCommand(categoryId, title, content, summary, coverImageUrl, metaTitle, metaDescription, tagIds);
    }
}
