package ir.sepahan.app.loyalty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Cursor پایدار برای Polling رویدادهای بیرونی (مثلاً خرید بلیط فوتبال از Django) --
 * یک ردیف به‌ازای هر منبع، تا بعد از Restart Backend هم Polling از همان‌جا ادامه پیدا کند (ADR-0014).
 */
@Entity
@Table(name = "external_poll_cursor", schema = "loyalty")
public class ExternalPollCursor {

    @Id
    @Column(length = 60)
    private String source;

    @Column(name = "last_cursor_at", nullable = false)
    private OffsetDateTime lastCursorAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ExternalPollCursor() {
        // JPA
    }

    public ExternalPollCursor(String source, OffsetDateTime lastCursorAt) {
        this.source = source;
        this.lastCursorAt = lastCursorAt;
    }

    public String getSource() {
        return source;
    }

    public OffsetDateTime getLastCursorAt() {
        return lastCursorAt;
    }

    public void setLastCursorAt(OffsetDateTime lastCursorAt) {
        this.lastCursorAt = lastCursorAt;
    }
}
