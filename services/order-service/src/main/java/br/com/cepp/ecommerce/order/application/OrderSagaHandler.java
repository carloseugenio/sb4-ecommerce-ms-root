package br.com.cepp.ecommerce.order.application;

import br.com.cepp.ecommerce.order.OrderRepository;
import br.com.cepp.ecommerce.order.OrderStatus;
import br.com.cepp.ecommerce.platform.domain.event.CheckoutEvents.PaymentAuthorized;
import br.com.cepp.ecommerce.platform.domain.event.CheckoutEvents.PaymentFailed;
import br.com.cepp.ecommerce.platform.domain.event.CheckoutEvents.StockRejected;
import br.com.cepp.ecommerce.platform.domain.event.EventEnvelope;
import br.com.cepp.ecommerce.platform.messaging.ProcessedMessages;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reacts to inventory/payment events. Each method is idempotent and runs in one transaction with its outbox write. */
@Service
public class OrderSagaHandler {

    static final String CONSUMER = "order-service";
    private static final Logger log = LoggerFactory.getLogger(OrderSagaHandler.class);

    private final OrderRepository orders;
    private final ProcessedMessages processed;
    private final OrderEvents events;

    OrderSagaHandler(OrderRepository orders, ProcessedMessages processed, OrderEvents events) {
        this.orders = orders;
        this.processed = processed;
        this.events = events;
    }

    @Transactional
    public void onPaymentAuthorized(EventEnvelope<PaymentAuthorized> event) {
        if (!processed.firstTime(event.eventId(), CONSUMER)) {
            return;
        }
        var order = orders.findById(event.payload().orderId()).orElse(null);
        if (order == null) {
            log.warn("PaymentAuthorized for unknown order {}", event.payload().orderId());
        } else if (order.confirm()) {
            events.confirmed(order, event.correlationId(), event.eventId().toString());
        } else if (order.getStatus() == OrderStatus.CANCELLED) {
            events.paymentVoidRequested(order, event.payload().paymentId(), event.correlationId(),
                    event.eventId().toString());
        }
    }

    @Transactional
    public void onPaymentFailed(EventEnvelope<PaymentFailed> event) {
        cancel(event, event.payload().orderId(), "PAYMENT_FAILED");
    }

    @Transactional
    public void onStockRejected(EventEnvelope<StockRejected> event) {
        cancel(event, event.payload().orderId(), "OUT_OF_STOCK");
    }

    /** Cancels pending orders older than the timeout so inventory and payment can compensate. */
    @Transactional
    public int cancelStale(Duration timeout) {
        var stale = orders.findByStatusAndCreatedAtBefore(OrderStatus.PENDING, Instant.now().minus(timeout));
        stale.forEach(o -> {
            o.cancel("TIMEOUT");
            events.cancelled(o, null, null);
        });
        return stale.size();
    }

    private void cancel(EventEnvelope<?> event, UUID orderId, String reason) {
        if (!processed.firstTime(event.eventId(), CONSUMER)) {
            return;
        }
        orders.findById(orderId).ifPresentOrElse(order -> {
            if (order.cancel(reason)) {
                events.cancelled(order, event.correlationId(), event.eventId().toString());
            }
        }, () -> log.warn("{} for unknown order {}", event.type(), orderId));
    }
}