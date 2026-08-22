package ir.sepahan.app.loyalty;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record DjangoCompletedOrdersResponse(@JsonProperty("orders") List<DjangoCompletedOrder> orders) {
}
