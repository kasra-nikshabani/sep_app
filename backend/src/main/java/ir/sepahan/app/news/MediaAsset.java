package ir.sepahan.app.news;

import ir.sepahan.app.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * ذخیره‌سازی روی دیسک محلی (تصمیم صریح کارفرما، ADR-0013). {@code storagePath} مسیر
 * فیزیکی روی دیسک است (هرگز مستقیم به کاربر نشان داده نمی‌شود)، {@code url} مسیر عمومی Serve.
 */
@Entity
@Table(name = "media_asset", schema = "news")
public class MediaAsset extends BaseEntity {

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @Column(nullable = false)
    private String url;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    protected MediaAsset() {
        // JPA
    }

    public MediaAsset(String fileName, String storagePath, String url, String contentType, long sizeBytes) {
        this.fileName = fileName;
        this.storagePath = storagePath;
        this.url = url;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
    }

    public String getFileName() {
        return fileName;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public String getUrl() {
        return url;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }
}
