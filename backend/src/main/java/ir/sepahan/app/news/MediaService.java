package ir.sepahan.app.news;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * ذخیره‌سازی روی دیسک محلی (تصمیم صریح کارفرما در Phase 11، ADR-0013) -- بدون هیچ
 * اتصال به یک سرویس Object Storage واقعی. برای مقیاس چند-Instance/CDN در آینده، این
 * سرویس تنها نقطه‌ای است که باید عوض شود.
 *
 * امنیت: نام فایل ذخیره‌شده روی دیسک همیشه تولیدشده توسط خود سیستم است (UUID +
 * پسوند از یک نگاشت ثابت Content-Type)، هرگز مستقیم از نام فایل ورودی کاربر گرفته
 * نمی‌شود -- جلوگیری از Path Traversal یا اجرای فایل با پسوند دلخواه.
 */
@Service
public class MediaService {

    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif");

    private final MediaAssetRepository mediaAssetRepository;
    private final Path storageRoot;
    private final String publicBaseUrl;
    private final Set<String> allowedContentTypes;

    public MediaService(MediaAssetRepository mediaAssetRepository,
                         @Value("${sepahan.news.media.storage-path}") String storagePath,
                         @Value("${sepahan.news.media.public-base-url}") String publicBaseUrl,
                         @Value("#{'${sepahan.news.media.allowed-content-types}'.split(',')}") List<String> allowedContentTypes) {
        this.mediaAssetRepository = mediaAssetRepository;
        this.storageRoot = Path.of(storagePath).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl;
        this.allowedContentTypes = Set.copyOf(allowedContentTypes);
        try {
            Files.createDirectories(this.storageRoot);
        } catch (IOException e) {
            throw new UncheckedIOException("ایجاد پوشه‌ی ذخیره‌سازی رسانه ناموفق بود: " + this.storageRoot, e);
        }
    }

    public MediaAsset store(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !allowedContentTypes.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "فرمت فایل مجاز نیست -- فقط " + allowedContentTypes);
        }
        String extension = EXTENSION_BY_CONTENT_TYPE.get(contentType);
        String storedFileName = UUID.randomUUID() + extension;
        Path destination = storageRoot.resolve(storedFileName);

        try {
            file.transferTo(destination);
        } catch (IOException e) {
            throw new UncheckedIOException("ذخیره‌ی فایل رسانه ناموفق بود", e);
        }

        String url = publicBaseUrl + "/" + storedFileName;
        MediaAsset asset = new MediaAsset(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : storedFileName,
                destination.toString(), url, contentType, file.getSize());
        return mediaAssetRepository.save(asset);
    }
}
