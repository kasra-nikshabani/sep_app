package ir.sepahan.app.news;

import ir.sepahan.app.users.AppUserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** مرور اخبار منتشرشده -- برای هر کاربر احراز هویت‌شده (fan/vip/admin، طبق rbac-matrix.md). */
@RestController
@RequestMapping("/api/v1/news")
public class NewsController {

    private final ArticleRepository articleRepository;
    private final NewsCategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final AppUserRepository appUserRepository;

    public NewsController(ArticleRepository articleRepository, NewsCategoryRepository categoryRepository,
                           TagRepository tagRepository, AppUserRepository appUserRepository) {
        this.articleRepository = articleRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.appUserRepository = appUserRepository;
    }

    @GetMapping("/categories")
    public List<CategoryResponse> categories() {
        return categoryRepository.findByDeletedAtIsNull().stream().map(CategoryResponse::of).toList();
    }

    @GetMapping("/tags")
    public List<TagResponse> tags() {
        return tagRepository.findByDeletedAtIsNull().stream().map(TagResponse::of).toList();
    }

    /**
     * اگر {@code size} داده نشود، همه‌ی اخبار منتشرشده (رفتار قبلی)؛ وگرنه صفحه‌بندی (page از ۰).
     * اپ موبایل همیشه صفحه‌بندی می‌کند چون اخبار واردشده از سایت باشگاه هزاران مورد است.
     */
    @GetMapping("/articles")
    public List<ArticleSummaryResponse> articles(@RequestParam(required = false) UUID categoryId,
                                                  @RequestParam(required = false) Integer page,
                                                  @RequestParam(required = false) Integer size) {
        List<Article> articles;
        if (size == null) {
            articles = categoryId != null
                    ? articleRepository.findByStatusAndCategoryIdAndDeletedAtIsNullOrderByPublishedAtDesc(ArticleStatus.published, categoryId)
                    : articleRepository.findByStatusAndDeletedAtIsNullOrderByPublishedAtDesc(ArticleStatus.published);
        } else {
            Pageable pageable = PageRequest.of(Math.max(page == null ? 0 : page, 0), Math.min(Math.max(size, 1), 50),
                    Sort.by(Sort.Direction.DESC, "publishedAt").and(Sort.by(Sort.Direction.DESC, "id")));
            articles = categoryId != null
                    ? articleRepository.findByStatusAndCategoryIdAndDeletedAtIsNull(ArticleStatus.published, categoryId, pageable)
                    : articleRepository.findByStatusAndDeletedAtIsNull(ArticleStatus.published, pageable);
        }
        return articles.stream().map(ArticleSummaryResponse::of).toList();
    }

    @GetMapping("/articles/{slug}")
    public ArticleDetailResponse articleDetail(@PathVariable String slug) {
        Article article = articleRepository.findBySlugAndStatusAndDeletedAtIsNull(slug, ArticleStatus.published)
                .orElseThrow(() -> new ArticleNotFoundException(slug));
        String authorName = appUserRepository.findById(article.getAuthorId())
                .map(u -> u.getDisplayName())
                .orElse(null);
        return ArticleDetailResponse.of(article, authorName);
    }
}
