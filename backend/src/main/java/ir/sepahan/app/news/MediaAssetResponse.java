package ir.sepahan.app.news;

import java.util.UUID;

public record MediaAssetResponse(UUID id, String url, String contentType, long sizeBytes) {
    public static MediaAssetResponse of(MediaAsset asset) {
        return new MediaAssetResponse(asset.getId(), asset.getUrl(), asset.getContentType(), asset.getSizeBytes());
    }
}
