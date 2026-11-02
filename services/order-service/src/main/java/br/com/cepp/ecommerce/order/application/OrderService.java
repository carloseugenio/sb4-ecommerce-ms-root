package br.com.cepp.ecommerce.order.application;

import br.com.cepp.ecommerce.order.Order;
import br.com.cepp.ecommerce.order.OrderRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
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

    /** Creates the order and the OrderPlaced outbox event in one transaction; replays the result for a repeated key. */
    @Transactional
    public Order place(String idempotencyKey, PlaceOrderCommand command) {
        String hash = sha256(json.writeValueAsString(command));
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