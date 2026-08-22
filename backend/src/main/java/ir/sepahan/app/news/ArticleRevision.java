package ir.sepahan.app.news;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

/**
 * Append-only: هر ویرایش، حالت *قبلی* خبر را این‌جا Snapshot می‌کند، پیش از اعمال
 * تغییر روی خود Article -- عمداً بدون common.BaseEntity (مثل Reservation/OrderItem).
 */
@Entity
@Table(name = "article_revision", schema = "news")
public class ArticleRevision {

    @Id
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false)
    private Article article;

    @Column(nullable = false, length = 250)
    private String title;

    @Column
    private String summary;

    @Column(nullable = false)
    private String content;

    @Column(name = "edited_by", nullable = false)
    private UUID editedBy;

    @CreationTimestamp
    @Column(name = "edited_at", nullable = false, updatable = false)
    private OffsetDateTime editedAt;

    protected ArticleRevision() {
        // JPA
    }

    public ArticleRevision(Article article, String title, String summary, String content, UUID editedBy) {
        this.article = article;
        this.title = title;
        this.summary = summary;
        this.content = content;
        this.editedBy = editedBy;
    }

    public UUID getId() {
        return id;
    }

    public Article getArticle() {
        return article;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getContent() {
        return content;
    }

    public UUID getEditedBy() {
        return editedBy;
    }

    public OffsetDateTime getEditedAt() {
        return editedAt;
    }
}
