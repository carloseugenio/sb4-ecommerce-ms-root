package br.com.cepp.ecommerce.order;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.UUID;

@Embeddable
public record OrderItem(UUID productId, String productName, int quantity, BigDecimal unitPrice) {

    public OrderItem {
        if (productId == null || productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("productId and productName are required");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be >= 1");
        }
        if (unitPrice == null || unitPrice.signum() <= 0) {
            throw new IllegalArgumentException("unitPrice must be > 0");
        }
    }

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}