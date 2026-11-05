package br.com.cepp.ecommerce.order;

import br.com.cepp.ecommerce.platform.domain.money.MoneyPolicy;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
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
        refundedAmount = refundedAmount == null ? BigDecimal.ZERO : refundedAmount;
    }

    public OrderItem(UUID productId, String productName, int quantity, BigDecimal unitPrice) {
        this(productId, productName, quantity, unitPrice, null);
    }

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    /** What can still be refunded for this line: the subtotal rounded with the platform rounding to {@code scale} minus what was refunded. */
    public BigDecimal remaining(int scale) {
        return subtotal().setScale(scale, MoneyPolicy.ROUNDING)
                .subtract(refundedAmount.setScale(scale, MoneyPolicy.ROUNDING));
    }

    OrderItem withRefund(BigDecimal share, int scale) {
        return new OrderItem(productId, productName, quantity, unitPrice,
                refundedAmount.setScale(scale, MoneyPolicy.ROUNDING).add(share));
    }
}