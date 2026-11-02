package br.com.cepp.ecommerce.platform.domain.event;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Payload contracts of the checkout saga. Services reference each other by ID only. */
public final class CheckoutEvents {

    private CheckoutEvents() {
    }

    public record OrderLine(UUID productId, int quantity) {
    }

    public record OrderPlaced(UUID orderId, UUID customerId, List<OrderLine> lines,
                              BigDecimal totalAmount, String currency) {
    }

    public record OrderConfirmed(UUID orderId) {
    }

    public record OrderCancelled(UUID orderId, String reason) {
    }

    public record StockReserved(UUID orderId, BigDecimal totalAmount, String currency) {
    }

    public record StockRejected(UUID orderId, String reason) {
    }

    public record StockReleased(UUID orderId) {
    }

    public record PaymentAuthorized(UUID orderId, UUID paymentId) {
    }

    public record PaymentFailed(UUID orderId, String reason) {
    }

    public record PaymentRefunded(UUID orderId, UUID paymentId) {
    }
}