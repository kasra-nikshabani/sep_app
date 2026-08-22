package ir.sepahan.app.news;

import java.util.Set;
import java.util.UUID;

/** فیلدهای مشترک ساخت/ویرایش خبر. {@code slug} عمداً این‌جا نیست -- بعد از ساخت غیرقابل‌ویرایش است. */
public record ArticleWriteCommand(
        UUID categoryId,
        String title,
        String content,
        String summary,
        String coverImageUrl,
        String metaTitle,
        String metaDescription,
        Set<UUID> tagIds
) {
}
