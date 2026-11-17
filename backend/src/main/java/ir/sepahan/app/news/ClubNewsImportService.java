package ir.sepahan.app.news;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ذخیره‌ی یک خبر اسکرپ‌شده به‌صورت published. عمداً Bean جدا از Job است: {@code @Transactional}
 * فقط از طریق Proxy کار می‌کند و Self-Invocation آن را دور می‌زند (همان درس ADR-0018).
 */
@Service
@ConditionalOnProperty(name = "sepahan.news.scraper.enabled", havingValue = "true")
public class ClubNewsImportService {

    /** نویسنده‌ی خبرهای واردشده؛ کاربر واقعی نیست (author_id بدون FK است) و نامی برای نمایش ندارد. */
    static final UUID IMPORT_AUTHOR_ID = new UUID(0L, 0L);
    static final String CATEGORY_SLUG = "club-news";
    static final String CATEGORY_NAME = "اخبار باشگاه";

    private final ArticleRepository articleRepository;
    private final NewsCategoryRepository categoryRepository;
    private final TagRepository tagRepository;

    public ClubNewsImportService(ArticleRepository articleRepository, NewsCategoryRepository categoryRepository,
                                  TagRepository tagRepository) {
        this.articleRepository = articleRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
    }

    @Transactional
    public void importArticle(SepahanscClient.ScrapedArticle scraped) {
        NewsCategory category = categoryRepository.findBySlugAndDeletedAtIsNull(CATEGORY_SLUG)
                .orElseGet(() -> categoryRepository.save(new NewsCategory(CATEGORY_NAME, CATEGORY_SLUG)));

        String title = truncate(scraped.title(), 250);
        Article article = new Article(category, IMPORT_AUTHOR_ID, title, "sepahansc-" + scraped.sourceId(), scraped.content());
        article.setSummary(scraped.summary());
        article.setCoverImageUrl(scraped.coverImageUrl());
        article.setMetaTitle(title);
        article.setMetaDescription(truncate(scraped.summary(), 500));
        article.setStatus(ArticleStatus.published);
        article.setPublishedAt(scraped.publishedAt() != null ? scraped.publishedAt() : OffsetDateTime.now());
        article.setSource(scraped.sourceId(), truncate(scraped.url(), 600));

        Set<Tag> tags = new HashSet<>();
        for (String name : scraped.tags()) {
            String slug = name.trim().replaceAll("\\s+", "-");
            tags.add(tagRepository.findBySlugAndDeletedAtIsNull(slug)
                    .orElseGet(() -> tagRepository.save(new Tag(name.trim(), slug))));
        }
        article.setTags(tags);

        articleRepository.save(article);
    }

    private static String truncate(String text, int max) {
        return text == null || text.length() <= max ? text : text.substring(0, max);
    }
}
