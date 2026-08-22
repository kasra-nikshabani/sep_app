package ir.sepahan.app.shop;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** مدیریت کاتالوگ (Category/Product/Variant) -- فقط نقش admin (طبق rbac-matrix.md). */
@Service
public class CatalogService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    public CatalogService(CategoryRepository categoryRepository, ProductRepository productRepository,
                           ProductVariantRepository productVariantRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
    }

    public Category createCategory(String name, String slug, UUID parentId) {
        Category parent = null;
        if (parentId != null) {
            parent = categoryRepository.findById(parentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "دسته‌بندی والد یافت نشد"));
        }
        return categoryRepository.save(new Category(name, slug, parent));
    }

    public Product createProduct(UUID categoryId, String name, String slug, String description, String imageUrl,
                                  BigDecimal basePrice) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "دسته‌بندی یافت نشد"));
        Product product = new Product(category, name, slug, basePrice);
        product.setDescription(description);
        product.setImageUrl(imageUrl);
        return productRepository.save(product);
    }

    public ProductVariant addVariant(UUID productId, String sku, Map<String, String> attributes,
                                      int initialStock, int weightGrams, BigDecimal priceOverride) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "محصول یافت نشد"));
        ProductVariant variant = new ProductVariant(product, sku, attributes, initialStock);
        variant.setWeightGrams(weightGrams);
        variant.setPriceOverride(priceOverride);
        return productVariantRepository.save(variant);
    }

    /** افزایش/کاهش دستی موجودی (مثلاً وارد کردن محموله‌ی جدید یا اصلاح شمارش انبار). */
    @Transactional
    public void adjustStock(UUID variantId, int delta) {
        productVariantRepository.findByIdAndDeletedAtIsNull(variantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "این Variant یافت نشد"));
        if (delta >= 0) {
            productVariantRepository.incrementStock(variantId, delta);
        } else {
            int affected = productVariantRepository.decrementStock(variantId, -delta);
            if (affected == 0) {
                throw new InsufficientStockException("(اصلاح دستی موجودی منفی نمی‌شود)");
            }
        }
    }

    public void setProductActive(UUID productId, boolean active) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "محصول یافت نشد"));
        product.setActive(active);
        productRepository.save(product);
    }
}
