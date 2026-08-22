package ir.sepahan.app.news;

import java.util.UUID;

public record CategoryResponse(UUID id, String name, String slug) {
    public static CategoryResponse of(NewsCategory category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getSlug());
    }
}
