package br.com.cepp.ecommerce.order;

import br.com.cepp.ecommerce.platform.domain.refund.RefundAllocator;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Order aggregate. State changes only through place/confirm/cancel; status is PENDING -> CONFIRMED | CANCELLED. */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    private UUID id;
    private UUID customerId;
    private String currency;
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private String cancelReason;

    private BigDecimal refundedAmount = BigDecimal.ZERO.setScale(2);
    @ElementCollection
    @CollectionTable(name = "order_item", joinColumns = @JoinColumn(name = "order_id"))
    private List<OrderItem> items = new ArrayList<>();

    @Version
    private long version;

    @Column(updatable = false)
    private Instant createdAt;
    private Instant updatedAt;

    protected Order() {
    }

    public static Order place(UUID customerId, String currency, List<OrderItem> items) {
        if (customerId == null || currency == null || currency.length() != 3) {
            throw new IllegalArgumentException("customerId and a 3-letter currency are required");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("an order needs at least one item");
        }
        Order order = new Order();
        order.id = UUID.randomUUID();
        order.customerId = customerId;
        order.currency = currency;
        order.items = new ArrayList<>(items);
        order.totalAmount = items.stream().map(OrderItem::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        order.status = OrderStatus.PENDING;
        order.createdAt = Instant.now();
        order.updatedAt = order.createdAt;
        return order;
    }

    /** @return true if the state changed; false if the order is not pending (duplicate or late event). */
    public boolean confirm() {
        if (status != OrderStatus.PENDING) {
            return false;
        }
        status = OrderStatus.CONFIRMED;
        updatedAt = Instant.now();
        return true;
    }

    /** @return true if the state changed; false if the order is not pending (duplicate or late event). */
    public boolean cancel(String reason) {
        if (status != OrderStatus.PENDING) {
            return false;
        }
        status = OrderStatus.CANCELLED;
        cancelReason = reason;
        updatedAt = Instant.now();
        return true;
    }

    /**
     * Refunds part of a confirmed order. The amount is split over the lines in proportion to what each line can
     * still be refunded. Moves to REFUNDED once the whole total has been refunded.
     *
     * @return the share given to each item, in item order, all with scale 2
     * @throws IllegalStateException    if the order is not CONFIRMED
     * @throws IllegalArgumentException if the amount is invalid or larger than what is left to refund
     */
    public List<BigDecimal> refund(BigDecimal amount) {
        if (status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Only a confirmed order can be refunded, but it is " + status);
        }
        List<BigDecimal> shares = RefundAllocator.allocate(amount,
                items.stream().map(OrderItem::remaining).toList());
        for (int i = 0; i < items.size(); i++) {
            items.set(i, items.get(i).withRefund(shares.get(i)));
        }
        refundedAmount = refundedAmount.add(amount.setScale(2, RoundingMode.UNNECESSARY));
        if (refundedAmount.compareTo(totalAmount) == 0) {
            status = OrderStatus.REFUNDED;
        }
        updatedAt = Instant.now();
        return shares;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    /** Total refunded so far, always with scale 2. */
    public BigDecimal getRefundedAmount() {
        return refundedAmount;
    }

    public List<OrderItem> getItems() {
        return List.copyOf(items);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}