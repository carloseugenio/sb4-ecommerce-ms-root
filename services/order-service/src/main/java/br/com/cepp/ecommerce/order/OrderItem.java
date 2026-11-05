package br.com.cepp.ecommerce.order;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Embeddable
public record OrderItem(UUID productId, String productName, int quantity, BigDecimal unitPrice,
                        BigDecimal refundedAmount) {

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
        refundedAmount = refundedAmount == null ? BigDecimal.ZERO.setScale(2)
                : refundedAmount.setScale(2, RoundingMode.HALF_UP);
    }

    public OrderItem(UUID productId, String productName, int quantity, BigDecimal unitPrice) {
        this(productId, productName, quantity, unitPrice, null);
    }

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    /** What can still be refunded for this line, with scale 2. */
    public BigDecimal remaining() {
        return subtotal().setScale(2, RoundingMode.HALF_UP).subtract(refundedAmount);
    }

    OrderItem withRefund(BigDecimal share) {
        return new OrderItem(productId, productName, quantity, unitPrice, refundedAmount.add(share));
    }
}