package ir.sepahan.app.news;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

/**
 * این تست‌ها به Postgres واقعی نیاز دارند (طبق backend/README.md)، هم‌سبک
 * OrderServiceTest/ReservationServiceTest (Phase 8-10).
 */
@SpringBootTest
class ArticleServiceTest {

    @Autowired
    private NewsCategoryRepository categoryRepository;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private ArticleRepository articleRepository;
    @Autowired
    private ArticleRevisionRepository articleRevisionRepository;
    @Autowired
    private ArticleService articleService;

    private NewsCategory category;

    @BeforeEach
    void setUp() {
        category = categoryRepository.save(new NewsCategory("اخبار مسابقات", "matches-" + System.nanoTime()));
    }

    @AfterEach
    void tearDown() {
        articleRevisionRepository.deleteAll();
        articleRepository.deleteAll();
        tagRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    private ArticleWriteCommand command(UUID categoryId, Set<UUID> tagIds) {
        return new ArticleWriteCommand(categoryId, "سپاهان قهرمان شد", "متن کامل خبر ...", "خلاصه‌ی خبر",
                null, null, null, tagIds);
    }

    @Test
    void createArticle_startsAsDraft() {
        UUID authorId = UUID.randomUUID();
        Article article = articleService.createArticle(authorId, "slug-" + System.nanoTime(), command(category.getId(), null));

        assertThat(article.getStatus()).isEqualTo(ArticleStatus.draft);
        assertThat(article.getAuthorId()).isEqualTo(authorId);
        assertThat(article.getPublishedAt()).isNull();
    }

    @Test
    void publishNow_setsStatusAndPublishedAt() {
        Article article = articleService.createArticle(UUID.randomUUID(), "slug-" + System.nanoTime(), command(category.getId(), null));

        Article published = articleService.publishNow(article.getId());

        assertThat(published.getStatus()).isEqualTo(ArticleStatus.published);
        assertThat(published.getPublishedAt()).isNotNull();
    }

    @Test
    void publishNow_isIdempotent_keepsOriginalPublishedAt() {
        Article article = articleService.createArticle(UUID.randomUUID(), "slug-" + System.nanoTime(), command(category.getId(), null));
        Article firstPublish = articleService.publishNow(article.getId());
        OffsetDateTime firstPublishedAt = firstPublish.getPublishedAt();

        Article secondCall = articleService.publishNow(article.getId());

        // مقایسه‌ی instant، نه فیلدها -- firstPublishedAt مقدار Offset محلی (in-memory) دارد
        // و secondCall از دیتابیس با Offset UTC برگشته (دو نمایش متفاوت از یک لحظه)؛ همچنین
        // Postgres TIMESTAMPTZ دقت را به میکروثانیه گرد می‌کند، پس یک تلورانس کوچک لازم است.
        assertThat(secondCall.getPublishedAt().toInstant()).isCloseTo(firstPublishedAt.toInstant(), within(1, ChronoUnit.MILLIS));
    }

    @Test
    void publishNow_rejectsArchivedArticle() {
        Article article = articleService.createArticle(UUID.randomUUID(), "slug-" + System.nanoTime(), command(category.getId(), null));
        articleService.publishNow(article.getId());
        articleService.archive(article.getId());

        assertThatThrownBy(() -> articleService.publishNow(article.getId()))
                .isInstanceOf(ArticleStateException.class);
    }

    @Test
    void schedule_rejectsPastDate() {
        Article article = articleService.createArticle(UUID.randomUUID(), "slug-" + System.nanoTime(), command(category.getId(), null));

        assertThatThrownBy(() -> articleService.schedule(article.getId(), OffsetDateTime.now().minusHours(1)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void autoPublishScheduledArticles_publishesWhenDue_usingOriginalScheduledTime() {
        Article article = articleService.createArticle(UUID.randomUUID(), "slug-" + System.nanoTime(), command(category.getId(), null));
        articleService.schedule(article.getId(), OffsetDateTime.now().plusMinutes(5));
        // مستقیم روی دیتابیس زمان‌بندی را در گذشته می‌گذاریم -- بدون صبر واقعی
        OffsetDateTime due = OffsetDateTime.now().minusMinutes(1);
        setScheduledAtInPast(article.getId(), due);

        articleService.autoPublishScheduledArticles();

        Article afterJob = articleRepository.findById(article.getId()).orElseThrow();
        assertThat(afterJob.getStatus()).isEqualTo(ArticleStatus.published);
        // isEqualToIgnoringNanos فیلدها را مستقیم مقایسه می‌کند، نه instant را -- با Offsetهای
        // متفاوت (این‌جا UTC از دیتابیس در برابر Offset محلی +۰۳:۳۰) نتیجه‌ی غلط می‌دهد؛
        // مقایسه‌ی instant با یک تلورانس کوچک (گرد شدن Postgres به میکروثانیه) درست است.
        assertThat(afterJob.getPublishedAt().toInstant()).isCloseTo(due.toInstant(), within(1, ChronoUnit.MILLIS));
    }

    private void setScheduledAtInPast(UUID articleId, OffsetDateTime when) {
        Article article = articleRepository.findById(articleId).orElseThrow();
        article.setScheduledAt(when);
        articleRepository.save(article);
    }

    @Test
    void updateArticle_createsRevisionSnapshot_ofPreviousContent() {
        Article article = articleService.createArticle(UUID.randomUUID(), "slug-" + System.nanoTime(), command(category.getId(), null));
        String originalTitle = article.getTitle();

        UUID editorId = UUID.randomUUID();
        ArticleWriteCommand updated = new ArticleWriteCommand(category.getId(), "عنوان ویرایش‌شده",
                "متن ویرایش‌شده", "خلاصه‌ی جدید", null, null, null, null);
        articleService.updateArticle(article.getId(), editorId, updated);

        var revisions = articleRevisionRepository.findByArticleIdOrderByEditedAtDesc(article.getId());
        assertThat(revisions).hasSize(1);
        assertThat(revisions.get(0).getTitle()).isEqualTo(originalTitle);
        assertThat(revisions.get(0).getEditedBy()).isEqualTo(editorId);

        Article afterUpdate = articleRepository.findById(article.getId()).orElseThrow();
        assertThat(afterUpdate.getTitle()).isEqualTo("عنوان ویرایش‌شده");
    }

    @Test
    void archive_thenSoftDelete_removesFromActiveLookup() {
        Article article = articleService.createArticle(UUID.randomUUID(), "slug-" + System.nanoTime(), command(category.getId(), null));
        articleService.publishNow(article.getId());
        articleService.archive(article.getId());
        assertThat(articleRepository.findById(article.getId()).orElseThrow().getStatus()).isEqualTo(ArticleStatus.archived);

        articleService.softDelete(article.getId());

        assertThat(articleRepository.findByIdAndDeletedAtIsNull(article.getId())).isEmpty();
    }

    @Test
    void createArticle_withTags_resolvesOnlyExistingTags() {
        Tag tag1 = tagRepository.save(new Tag("فوتبال", "football-" + System.nanoTime()));
        UUID nonExistentTagId = UUID.randomUUID();

        Article article = articleService.createArticle(UUID.randomUUID(), "slug-" + System.nanoTime(),
                command(category.getId(), Set.of(tag1.getId(), nonExistentTagId)));

        assertThat(article.getTags()).extracting(Tag::getId).containsExactly(tag1.getId());
    }
}
