package ir.sepahan.app.shop;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** برای هر عملیاتی که وضعیت فعلی سفارش/درخواست مرجوعی اجازه‌اش را نمی‌دهد. */
@ResponseStatus(HttpStatus.CONFLICT)
public class OrderStateException extends RuntimeException {

    public OrderStateException(String message) {
        super(message);
    }
}
