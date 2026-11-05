package br.com.cepp.ecommerce.order.application;

import br.com.cepp.ecommerce.order.Order;
import br.com.cepp.ecommerce.order.OrderRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
public class OrderService {

    private final OrderRepository orders;
    private final IdempotencyStore idempotency;
    private final OrderEvents events;
    private final JsonMapper json;

    OrderService(OrderRepository orders, IdempotencyStore idempotency, OrderEvents events, JsonMapper json) {
        this.orders = orders;
        this.idempotency = idempotency;
        this.events = events;
        this.json = json;
    }

    @Transactional
    public Order refund(UUID id, BigDecimal amount) {
        return refund(id, amount, null);
    }

    /**
     * Refunds part of an order. With an idempotency key, a repeated request (same order, numerically equal amount)
     * returns the order as it is now without refunding again, even when the order has meanwhile become REFUNDED.
     */
    @Transactional
    public Order refund(UUID id, BigDecimal amount, String idempotencyKey) {
        String hash = amount == null ? null : sha256("refund|" + id + "|" + amount.stripTrailingZeros().toPlainString());
        if (idempotencyKey != null && hash != null) {
            var stored = idempotency.find(idempotencyKey);
            if (stored.isPresent()) {
                if (!stored.get().requestHash().equals(hash) || !id.equals(stored.get().orderId())) {
                    throw new IdempotencyConflictException(idempotencyKey);
                }
                return orders.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
            }
        }
        Order order = orders.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
        var shares = order.refund(amount);
        if (idempotencyKey != null) {
            idempotency.save(idempotencyKey, hash, order.getId());
        }
        events.refunded(order, shares);
        return order;
    }

    /** Takes back the latest refund still in effect; with a key, a repeated request returns the order unchanged. */
    @Transactional
    public Order undoRefund(UUID id, String idempotencyKey) {
        return reverse(id, idempotencyKey, "undo");
    }

    /** Applies the oldest undone refund again; with a key, a repeated request returns the order unchanged. */
    @Transactional
    public Order redoRefund(UUID id, String idempotencyKey) {
        return reverse(id, idempotencyKey, "redo");
    }

    private Order reverse(UUID id, String idempotencyKey, String kind) {
        String hash = sha256(kind + "|" + id);
        if (idempotencyKey != null) {
            var stored = idempotency.find(idempotencyKey);
            if (stored.isPresent()) {
                if (!stored.get().requestHash().equals(hash) || !id.equals(stored.get().orderId())) {
                    throw new IdempotencyConflictException(idempotencyKey);
                }
                return orders.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
            }
        }
        Order order = orders.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
        if (kind.equals("undo")) {
            events.refundReversed(order, order.undoRefund());
        } else {
            events.refunded(order, order.redoRefund());
        }
        if (idempotencyKey != null) {
            idempotency.save(idempotencyKey, hash, order.getId());
        }
        return order;
    }

    private record PlaceItemHash(UUID productId, String productName, int quantity, BigDecimal unitPrice) {
    }

    private record PlaceHash(UUID customerId, String currency, List<PlaceItemHash> items) {
    }

    /** Creates the order and the OrderPlaced outbox event in one transaction; replays the result for a repeated key. */
    @Transactional
    public Order place(String idempotencyKey, PlaceOrderCommand command) {
        String hash = sha256(json.writeValueAsString(new PlaceHash(command.customerId(), command.currency(),
                command.items().stream().map(i -> new PlaceItemHash(i.productId(), i.productName(), i.quantity(),
                        i.unitPrice())).toList())));
        var stored = idempotency.find(idempotencyKey);
        if (stored.isPresent()) {
            if (!stored.get().requestHash().equals(hash)) {
                throw new IdempotencyConflictException(idempotencyKey);
            }
            return orders.findById(stored.get().orderId()).orElseThrow();
        }
        Order order = orders.save(Order.place(command.customerId(), command.currency(), command.items()));
        idempotency.save(idempotencyKey, hash, order.getId());
        events.placed(order, idempotencyKey);
        return order;
    }

    @Transactional(readOnly = true)
    public Order get(UUID id) {
        return orders.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}