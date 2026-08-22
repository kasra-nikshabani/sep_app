package ir.sepahan.app.shop;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CouponInvalidException extends RuntimeException {

    public CouponInvalidException(String message) {
        super(message);
    }
}
