package ir.sepahan.app.news;

import org.springframework.stereotype.Service;

/** مدیریت دسته‌بندی/برچسب اخبار -- فقط نقش admin (طبق rbac-matrix.md). */
@Service
public class NewsCatalogService {

    private final NewsCategoryRepository categoryRepository;
    private final TagRepository tagRepository;

    public NewsCatalogService(NewsCategoryRepository categoryRepository, TagRepository tagRepository) {
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
    }

    public NewsCategory createCategory(String name, String slug) {
        return categoryRepository.save(new NewsCategory(name, slug));
    }

    public Tag createTag(String name, String slug) {
        return tagRepository.save(new Tag(name, slug));
    }
}
