package ir.sepahan.app.loyalty;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class RedemptionStateException extends RuntimeException {

    public RedemptionStateException(String message) {
        super(message);
    }
}
