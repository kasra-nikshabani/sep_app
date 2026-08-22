package ir.sepahan.app.news;

import java.util.UUID;

public record TagResponse(UUID id, String name, String slug) {
    public static TagResponse of(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName(), tag.getSlug());
    }
}
