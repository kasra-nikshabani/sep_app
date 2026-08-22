package ir.sepahan.app.shop;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class CartEmptyException extends RuntimeException {

    public CartEmptyException() {
        super("سبد خرید شما خالی است");
    }
}
