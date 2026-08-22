package ir.sepahan.app.news;

import ir.sepahan.app.users.AppUser;
import ir.sepahan.app.users.AppUserRepository;
import ir.sepahan.app.users.UserProvisioningService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * مدیریت کامل خبر (Create/Edit/Publish/Schedule/Draft/Revision) + دسته‌بندی/برچسب/رسانه --
 * فقط نقش admin (طبق rbac-matrix.md). پیاده‌سازی حداقلی برای این فاز؛ Admin Panel واقعی
 * موضوع Phase 14 است (همان الگوی TicketingAdminController/ShopAdminController).
 */
@RestController
@RequestMapping("/api/v1/news/admin")
@PreAuthorize("hasRole('admin')")
public class NewsAdminController {

    private final NewsCatalogService newsCatalogService;
    private final ArticleService articleService;
    private final ArticleRepository articleRepository;
    private final ArticleRevisionRepository articleRevisionRepository;
    private final MediaService mediaService;
    private final AppUserRepository appUserRepository;
    private final UserProvisioningService userProvisioningService;

    public NewsAdminController(NewsCatalogService newsCatalogService, ArticleService articleService,
                                ArticleRepository articleRepository, ArticleRevisionRepository articleRevisionRepository,
                                MediaService mediaService, AppUserRepository appUserRepository,
                                UserProvisioningService userProvisioningService) {
        this.newsCatalogService = newsCatalogService;
        this.articleService = articleService;
        this.articleRepository = articleRepository;
        this.articleRevisionRepository = articleRevisionRepository;
        this.mediaService = mediaService;
        this.appUserRepository = appUserRepository;
        this.userProvisioningService = userProvisioningService;
    }

    @PostMapping("/categories")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        NewsCategory category = newsCatalogService.createCategory(request.name(), request.slug());
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoryResponse.of(category));
    }

    @PostMapping("/tags")
    public ResponseEntity<TagResponse> createTag(@Valid @RequestBody CreateTagRequest request) {
        Tag tag = newsCatalogService.createTag(request.name(), request.slug());
        return ResponseEntity.status(HttpStatus.CREATED).body(TagResponse.of(tag));
    }

    @PostMapping("/media")
    public ResponseEntity<MediaAssetResponse> uploadMedia(@RequestParam("file") MultipartFile file) {
        MediaAsset asset = mediaService.store(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(MediaAssetResponse.of(asset));
    }

    @PostMapping("/articles")
    public ResponseEntity<ArticleDetailResponse> createArticle(@Valid @RequestBody CreateArticleRequest request,
                                                                 @AuthenticationPrincipal Jwt jwt) {
        Article article = articleService.createArticle(currentUserId(jwt), request.slug(), request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDetail(article));
    }

    @PutMapping("/articles/{articleId}")
    public ArticleDetailResponse updateArticle(@PathVariable UUID articleId, @Valid @RequestBody UpdateArticleRequest request,
                                                @AuthenticationPrincipal Jwt jwt) {
        Article article = articleService.updateArticle(articleId, currentUserId(jwt), request.toCommand());
        return toDetail(article);
    }

    @PostMapping("/articles/{articleId}/publish")
    public ArticleDetailResponse publish(@PathVariable UUID articleId) {
        return toDetail(articleService.publishNow(articleId));
    }

    @PostMapping("/articles/{articleId}/schedule")
    public ArticleDetailResponse schedule(@PathVariable UUID articleId, @Valid @RequestBody ScheduleArticleRequest request) {
        return toDetail(articleService.schedule(articleId, request.scheduledAt()));
    }

    @PostMapping("/articles/{articleId}/archive")
    public ArticleDetailResponse archive(@PathVariable UUID articleId) {
        return toDetail(articleService.archive(articleId));
    }

    @DeleteMapping("/articles/{articleId}")
    public ResponseEntity<Void> delete(@PathVariable UUID articleId) {
        articleService.softDelete(articleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/articles/{articleId}")
    public ArticleDetailResponse articleDetail(@PathVariable UUID articleId) {
        Article article = articleRepository.findByIdAndDeletedAtIsNull(articleId)
                .orElseThrow(() -> new ArticleNotFoundException(articleId));
        return toDetail(article);
    }

    @GetMapping("/articles")
    public List<ArticleSummaryResponse> allArticles() {
        return articleRepository.findByDeletedAtIsNullOrderByCreatedAtDesc().stream()
                .map(ArticleSummaryResponse::of)
                .toList();
    }

    @GetMapping("/articles/{articleId}/revisions")
    public List<ArticleRevisionResponse> revisions(@PathVariable UUID articleId) {
        return articleRevisionRepository.findByArticleIdOrderByEditedAtDesc(articleId).stream()
                .map(ArticleRevisionResponse::of)
                .toList();
    }

    private ArticleDetailResponse toDetail(Article article) {
        String authorName = appUserRepository.findById(article.getAuthorId())
                .map(AppUser::getDisplayName)
                .orElse(null);
        return ArticleDetailResponse.of(article, authorName);
    }

    private UUID currentUserId(Jwt jwt) {
        AppUser user = userProvisioningService.ensureUserForToken(jwt);
        return user.getId();
    }
}
