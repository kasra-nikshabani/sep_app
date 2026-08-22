package ir.sepahan.app.shop;

import java.util.UUID;

public record CategoryResponse(UUID id, String name, String slug, UUID parentId) {
    public static CategoryResponse of(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getSlug(),
                category.getParent() != null ? category.getParent().getId() : null);
    }
}
