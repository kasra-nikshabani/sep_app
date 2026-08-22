package ir.sepahan.app.news;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ArticleStateException extends RuntimeException {

    public ArticleStateException(String message) {
        super(message);
    }
}
