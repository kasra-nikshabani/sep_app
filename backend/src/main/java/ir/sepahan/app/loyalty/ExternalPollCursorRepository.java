package ir.sepahan.app.loyalty;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalPollCursorRepository extends JpaRepository<ExternalPollCursor, String> {
}
