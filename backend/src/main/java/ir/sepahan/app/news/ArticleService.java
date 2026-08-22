package ir.sepahan.app.news;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * چرخه‌ی عمر خبر: draft -> scheduled -> published -> archived (یا draft/scheduled -> published
 * مستقیم). هر ویرایش محتوا، حالت قبلی را در {@link ArticleRevision} Snapshot می‌کند --
 * طبق بند ۱۷ بریف («Revision»).
 */
@Service
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final ArticleRevisionRepository articleRevisionRepository;
    private final NewsCategoryRepository categoryRepository;
    private final TagRepository tagRepository;

    public ArticleService(ArticleRepository articleRepository, ArticleRevisionRepository articleRevisionRepository,
                           NewsCategoryRepository categoryRepository, TagRepository tagRepository) {
        this.articleRepository = articleRepository;
        this.articleRevisionRepository = articleRevisionRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
    }

    /** slug عمداً جدا از ArticleWriteCommand است -- بعد از ساخت، دیگر قابل ویرایش نیست (پایداری URL/SEO). */
    @Transactional
    public Article createArticle(UUID authorId, String slug, ArticleWriteCommand command) {
        NewsCategory category = requireCategory(command.categoryId());
        Article article = new Article(category, authorId, command.title(), slug, command.content());
        applyOptionalFields(article, command);
        return articleRepository.save(article);
    }

    @Transactional
    public Article updateArticle(UUID articleId, UUID editorId, ArticleWriteCommand command) {
        Article article = requireArticle(articleId);
        articleRevisionRepository.save(
                new ArticleRevision(article, article.getTitle(), article.getSummary(), article.getContent(), editorId));

        article.setCategory(requireCategory(command.categoryId()));
        article.setTitle(command.title());
        article.setContent(command.content());
        applyOptionalFields(article, command);
        return articleRepository.save(article);
    }

    private void applyOptionalFields(Article article, ArticleWriteCommand command) {
        article.setSummary(command.summary());
        article.setCoverImageUrl(command.coverImageUrl());
        article.setMetaTitle(command.metaTitle());
        article.setMetaDescription(command.metaDescription());
        article.setTags(resolveTags(command.tagIds()));
    }

    /**
     * همیشه یک HashSet قابل‌تغییر برمی‌گرداند -- Set.of() تغییرناپذیر است و وقتی
     * Hibernate حین Merge سعی می‌کند کالکشن مدیریت‌شده‌ی قبلی را با این مقدار
     * جایگزین کند (که نیازمند clear() داخلی است)، با UnsupportedOperationException
     * شکست می‌خورد -- این باگ واقعی در تست همین فاز کشف شد.
     */
    private Set<Tag> resolveTags(Set<UUID> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(tagRepository.findByIdInAndDeletedAtIsNull(tagIds));
    }

    /** Idempotent: اگر از قبل published است، بدون تغییر publishedAt برمی‌گردد -- تاریخ اولین انتشار حفظ می‌شود. */
    @Transactional
    public Article publishNow(UUID articleId) {
        Article article = requireArticle(articleId);
        if (article.getStatus() == ArticleStatus.published) {
            return article;
        }
        if (article.getStatus() == ArticleStatus.archived) {
            throw new ArticleStateException("خبر بایگانی‌شده را نمی‌توان مستقیم منتشر کرد");
        }
        article.setStatus(ArticleStatus.published);
        article.setPublishedAt(OffsetDateTime.now());
        article.setScheduledAt(null);
        return articleRepository.save(article);
    }

    @Transactional
    public Article schedule(UUID articleId, OffsetDateTime scheduledAt) {
        if (scheduledAt.isBefore(OffsetDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "زمان انتشار زمان‌بندی‌شده باید در آینده باشد");
        }
        Article article = requireArticle(articleId);
        if (article.getStatus() == ArticleStatus.published || article.getStatus() == ArticleStatus.archived) {
            throw new ArticleStateException("فقط خبر پیش‌نویس یا زمان‌بندی‌شده را می‌توان دوباره زمان‌بندی کرد");
        }
        article.setStatus(ArticleStatus.scheduled);
        article.setScheduledAt(scheduledAt);
        return articleRepository.save(article);
    }

    @Transactional
    public Article archive(UUID articleId) {
        Article article = requireArticle(articleId);
        if (article.getStatus() == ArticleStatus.archived) {
            return article;
        }
        article.setStatus(ArticleStatus.archived);
        return articleRepository.save(article);
    }

    @Transactional
    public void softDelete(UUID articleId) {
        Article article = requireArticle(articleId);
        article.setDeletedAt(OffsetDateTime.now());
        articleRepository.save(article);
    }

    /** انتشار خودکار خبرهای زمان‌بندی‌شده -- هم‌الگوی releaseExpiredOrders/releaseExpiredReservations. */
    @Scheduled(fixedDelayString = "${sepahan.news.publish-check-interval-ms:60000}")
    @Transactional
    public void autoPublishScheduledArticles() {
        for (Article article : articleRepository.findByStatusAndScheduledAtBefore(ArticleStatus.scheduled, OffsetDateTime.now())) {
            article.setStatus(ArticleStatus.published);
            article.setPublishedAt(article.getScheduledAt());
            articleRepository.save(article);
        }
    }

    private Article requireArticle(UUID articleId) {
        return articleRepository.findByIdAndDeletedAtIsNull(articleId)
                .orElseThrow(() -> new ArticleNotFoundException(articleId));
    }

    private NewsCategory requireCategory(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .filter(c -> c.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "دسته‌بندی یافت نشد"));
    }
}
