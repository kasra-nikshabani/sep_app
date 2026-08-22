package ir.sepahan.app.news;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ArticleNotFoundException extends RuntimeException {

    public ArticleNotFoundException(UUID articleId) {
        super("خبری با شناسه‌ی " + articleId + " یافت نشد");
    }

    public ArticleNotFoundException(String slug) {
        super("خبری با نشانی‌ی " + slug + " یافت نشد");
    }
}
