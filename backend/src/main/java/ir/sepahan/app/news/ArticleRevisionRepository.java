package ir.sepahan.app.news;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRevisionRepository extends JpaRepository<ArticleRevision, UUID> {

    List<ArticleRevision> findByArticleIdOrderByEditedAtDesc(UUID articleId);
}
