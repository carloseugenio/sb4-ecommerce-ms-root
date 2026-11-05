package br.com.cepp.ecommerce.order.application;

import br.com.cepp.ecommerce.order.Order;
import br.com.cepp.ecommerce.order.OrderStatus;
import br.com.cepp.ecommerce.platform.domain.event.CheckoutEvents.OrderRefunded;
import br.com.cepp.ecommerce.platform.domain.event.CheckoutEvents.PaymentVoidRequested;
import br.com.cepp.ecommerce.platform.domain.event.CheckoutEvents.RefundLine;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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

    void refunded(Order o, List<BigDecimal> shares) {
        var lines = new ArrayList<RefundLine>();
        for (int i = 0; i < shares.size(); i++) {
            if (shares.get(i).signum() > 0) {
                lines.add(new RefundLine(o.getItems().get(i).productId(), shares.get(i)));
            }
        }
        var amount = shares.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        outbox.record(Topics.ORDER_REFUNDED, EventEnvelope.of("OrderRefunded", o.getId().toString(),
                o.getId().toString(), null, new OrderRefunded(o.getId(), amount,
                        o.getRefundedAmount(), o.getCurrency(), o.getStatus() == OrderStatus.REFUNDED, lines)));
    }

    void paymentVoidRequested(Order o, UUID paymentId, String correlationId, String causationId) {
        outbox.record(Topics.PAYMENT_VOID_REQUESTED, EventEnvelope.of("PaymentVoidRequested", o.getId().toString(),
                correlationId, causationId, new PaymentVoidRequested(o.getId(), paymentId,
                        o.getTotalAmount().setScale(o.currencyScale(), RoundingMode.HALF_UP), o.getCurrency())));
    }
}