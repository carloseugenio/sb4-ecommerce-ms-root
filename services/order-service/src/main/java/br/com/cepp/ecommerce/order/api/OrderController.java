package br.com.cepp.ecommerce.order.api;

import br.com.cepp.ecommerce.order.Order;
import br.com.cepp.ecommerce.order.OrderItem;
import br.com.cepp.ecommerce.order.OrderStatus;
import br.com.cepp.ecommerce.order.application.OrderService;
import br.com.cepp.ecommerce.order.application.PlaceOrderCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
class OrderController {

    record ItemRequest(@NotNull UUID productId, @NotBlank String productName, @Min(1) int quantity,
                       @NotNull @DecimalMin("0.01") BigDecimal unitPrice) {
    }

    record PlaceOrderRequest(@NotNull UUID customerId, @NotBlank @Size(min = 3, max = 3) String currency,
                             @NotEmpty List<@Valid ItemRequest> items) {
    }

    record RefundRequest(@NotNull BigDecimal amount) {
    }

    record OrderResponse(UUID id, OrderStatus status, BigDecimal totalAmount, String currency, String cancelReason,
                         BigDecimal refundedAmount) {

        static OrderResponse from(Order o) {
            return new OrderResponse(o.getId(), o.getStatus(), o.getTotalAmount(), o.getCurrency(), o.getCancelReason(),
                    o.getRefundedAmount());
        }
    }

    private final OrderService service;

    OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    OrderResponse place(@RequestHeader("Idempotency-Key") @Size(min = 1, max = 100) String key,
                        @Valid @RequestBody PlaceOrderRequest request) {
        var items = request.items().stream()
                .map(i -> new OrderItem(i.productId(), i.productName(), i.quantity(), i.unitPrice())).toList();
        return OrderResponse.from(service.place(key, new PlaceOrderCommand(request.customerId(), request.currency(), items)));
    }

    @PostMapping("/{id}/refunds")
    OrderResponse refund(@PathVariable UUID id, @Valid @RequestBody RefundRequest request) {
        return OrderResponse.from(service.refund(id, request.amount()));
    }

    @GetMapping("/{id}")
    OrderResponse get(@PathVariable UUID id) {
        return OrderResponse.from(service.get(id));
    }
}