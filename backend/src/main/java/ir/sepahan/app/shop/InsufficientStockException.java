package ir.sepahan.app.shop;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String sku) {
        super("موجودی کالای " + sku + " کافی نیست");
    }
}
