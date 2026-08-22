package ir.sepahan.app.loyalty;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class InsufficientPointsException extends RuntimeException {

    public InsufficientPointsException() {
        super("امتیاز کافی برای این عملیات ندارید");
    }
}
