package ir.sepahan.app.shop;

import ir.sepahan.app.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "category", schema = "shop")
public class Category extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 150)
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY)
    private Category parent;

    protected Category() {
        // JPA
    }

    public Category(String name, String slug, Category parent) {
        this.name = name;
        this.slug = slug;
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public Category getParent() {
        return parent;
    }
}
