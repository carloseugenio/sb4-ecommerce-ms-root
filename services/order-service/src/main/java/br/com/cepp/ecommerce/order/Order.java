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

    private BigDecimal refundedAmount = BigDecimal.ZERO;
    @ElementCollection
    @CollectionTable(name = "order_item", joinColumns = @JoinColumn(name = "order_id"))
    private List<OrderItem> items = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "order_refund", joinColumns = @JoinColumn(name = "order_id"))
    private List<RefundEntry> refundLog = new ArrayList<>();

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
     * still be refunded, in the decimal places of the order currency. Moves to REFUNDED once no line has anything
     * left to refund.
     *
     * @return the share given to each item, in item order, all with the scale of the currency
     * @throws IllegalStateException    if the order is not CONFIRMED
     * @throws IllegalArgumentException if the amount is invalid or larger than what is left to refund
     */
    public List<BigDecimal> refund(BigDecimal amount) {
        if (status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Only a confirmed order can be refunded, but it is " + status);
        }
        int scale = currencyScale();
        List<BigDecimal> shares = RefundAllocator.allocate(amount,
                items.stream().map(i -> i.remaining(scale)).toList(), scale);
        refundLog.removeIf(RefundEntry::undone);
        int next = refundLog.stream().mapToInt(RefundEntry::seq).max().orElse(0) + 1;
        refundLog.add(RefundEntry.of(next, shares));
        apply(shares, scale);
        return shares;
    }

    /**
     * Takes back the most recent refund that is still in effect: every item and the running total give back exactly
     * the shares of that refund, and a REFUNDED order becomes CONFIRMED again.
     *
     * @return the shares that were taken back, in item order
     * @throws IllegalStateException if the order is not CONFIRMED or REFUNDED, or no refund is in effect
     */
    public List<BigDecimal> undoRefund() {
        if (status != OrderStatus.CONFIRMED && status != OrderStatus.REFUNDED) {
            throw new IllegalStateException("Only a confirmed or refunded order can have a refund undone, but it is " + status);
        }
        int index = -1;
        for (int i = 0; i < refundLog.size(); i++) {
            RefundEntry e = refundLog.get(i);
            if (!e.undone() && (index < 0 || e.seq() > refundLog.get(index).seq())) {
                index = i;
            }
        }
        if (index < 0) {
            throw new IllegalStateException("There is no refund to undo");
        }
        RefundEntry entry = refundLog.get(index);
        List<BigDecimal> shares = entry.shareList();
        refundLog.set(index, entry.withUndone(true));
        apply(shares.stream().map(BigDecimal::negate).toList(), currencyScale());
        return shares;
    }

    /**
     * Applies again the undone refund that was undone first (the oldest one), with exactly the shares it had. A new
     * refund discards everything that was undone before it.
     *
     * @return the shares that were applied, in item order
     * @throws IllegalStateException if the order is not CONFIRMED or nothing was undone
     */
    public List<BigDecimal> redoRefund() {
        if (status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Only a confirmed order can have a refund redone, but it is " + status);
        }
        int index = -1;
        for (int i = 0; i < refundLog.size(); i++) {
            RefundEntry e = refundLog.get(i);
            if (e.undone() && (index < 0 || e.seq() < refundLog.get(index).seq())) {
                index = i;
            }
        }
        if (index < 0) {
            throw new IllegalStateException("There is no undone refund to redo");
        }
        RefundEntry entry = refundLog.get(index);
        List<BigDecimal> shares = entry.shareList();
        refundLog.set(index, entry.withUndone(false));
        apply(shares, currencyScale());
        return shares;
    }

    private void apply(List<BigDecimal> shares, int scale) {
        for (int i = 0; i < items.size(); i++) {
            items.set(i, items.get(i).withRefund(shares.get(i), scale));
        }
        refundedAmount = getRefundedAmount().add(shares.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        boolean nothingLeft = items.stream().allMatch(i -> i.remaining(scale).signum() == 0);
        status = nothingLeft ? OrderStatus.REFUNDED : OrderStatus.CONFIRMED;
        updatedAt = Instant.now();
    }

    /** Decimal places of the order currency (2 for BRL, 0 for JPY, 3 for KWD); 2 if the code is not an ISO currency. */
    public int currencyScale() {
        try {
            int digits = java.util.Currency.getInstance(currency).getDefaultFractionDigits();
            return digits < 0 ? 2 : digits;
        } catch (IllegalArgumentException e) {
            return 2;
        }
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

    /** Total refunded so far, always with the scale of the order currency. */
    public BigDecimal getRefundedAmount() {
        return refundedAmount.setScale(currencyScale(), RoundingMode.HALF_UP);
    }

    public List<OrderItem> getItems() {
        return List.copyOf(items);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}