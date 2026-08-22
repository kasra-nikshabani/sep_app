package ir.sepahan.app.news;

import ir.sepahan.app.users.AppUserRepository;
import java.util.List;
import java.util.UUID;
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

    @GetMapping("/articles")
    public List<ArticleSummaryResponse> articles(@RequestParam(required = false) UUID categoryId) {
        List<Article> articles = categoryId != null
                ? articleRepository.findByStatusAndCategoryIdAndDeletedAtIsNullOrderByPublishedAtDesc(ArticleStatus.published, categoryId)
                : articleRepository.findByStatusAndDeletedAtIsNullOrderByPublishedAtDesc(ArticleStatus.published);
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
