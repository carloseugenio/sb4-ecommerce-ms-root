package br.com.cepp.ecommerce.order.application;

import br.com.cepp.ecommerce.order.Order;
import br.com.cepp.ecommerce.platform.domain.event.CheckoutEvents.OrderCancelled;
import br.com.cepp.ecommerce.platform.domain.event.CheckoutEvents.OrderConfirmed;
import br.com.cepp.ecommerce.platform.domain.event.CheckoutEvents.OrderLine;
import br.com.cepp.ecommerce.platform.domain.event.CheckoutEvents.OrderPlaced;
import br.com.cepp.ecommerce.platform.domain.event.EventEnvelope;
import br.com.cepp.ecommerce.platform.domain.event.Topics;
import br.com.cepp.ecommerce.platform.messaging.OutboxRecorder;
import org.springframework.stereotype.Component;

/** Records the order events in the outbox, within the caller's transaction. */
@Component
class OrderEvents {

    private final OutboxRecorder outbox;

    OrderEvents(OutboxRecorder outbox) {
        this.outbox = outbox;
    }

    void placed(Order o, String correlationId) {
        var lines = o.getItems().stream().map(i -> new OrderLine(i.productId(), i.quantity())).toList();
        outbox.record(Topics.ORDER_PLACED, EventEnvelope.of("OrderPlaced", o.getId().toString(), correlationId, null,
                new OrderPlaced(o.getId(), o.getCustomerId(), lines, o.getTotalAmount(), o.getCurrency())));
    }

    void confirmed(Order o, String correlationId, String causationId) {
        outbox.record(Topics.ORDER_CONFIRMED, EventEnvelope.of("OrderConfirmed", o.getId().toString(),
                correlationId, causationId, new OrderConfirmed(o.getId())));
    }

    void cancelled(Order o, String correlationId, String causationId) {
        outbox.record(Topics.ORDER_CANCELLED, EventEnvelope.of("OrderCancelled", o.getId().toString(),
                correlationId, causationId, new OrderCancelled(o.getId(), o.getCancelReason())));
    }
}