package ir.sepahan.app.news;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, UUID> {

    List<Tag> findByDeletedAtIsNull();

    Set<Tag> findByIdInAndDeletedAtIsNull(Set<UUID> ids);

    Optional<Tag> findBySlugAndDeletedAtIsNull(String slug);
}
