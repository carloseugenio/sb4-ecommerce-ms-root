package br.com.cepp.ecommerce.order.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.cepp.ecommerce.order.Order;
import br.com.cepp.ecommerce.order.OrderItem;
import br.com.cepp.ecommerce.order.OrderRepository;
import br.com.cepp.ecommerce.order.OrderStatus;
import br.com.cepp.ecommerce.platform.domain.event.CheckoutEvents.PaymentAuthorized;
import br.com.cepp.ecommerce.platform.domain.event.CheckoutEvents.PaymentVoidRequested;
import br.com.cepp.ecommerce.platform.domain.event.EventEnvelope;
import br.com.cepp.ecommerce.platform.domain.event.Topics;
import br.com.cepp.ecommerce.platform.messaging.OutboxRecorder;
import br.com.cepp.ecommerce.platform.messaging.ProcessedMessages;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OrderVoidSagaTest {

    private final OrderRepository orders = mock(OrderRepository.class);
    private final ProcessedMessages processed = mock(ProcessedMessages.class);
    private final OutboxRecorder outbox = mock(OutboxRecorder.class);
    private final OrderSagaHandler handler = new OrderSagaHandler(orders, processed, new OrderEvents(outbox));

    private Order order() {
        Order o = Order.place(UUID.randomUUID(), "BRL",
                List.of(new OrderItem(UUID.randomUUID(), "A", 1, new BigDecimal("7"))));
        when(orders.findById(o.getId())).thenReturn(Optional.of(o));
        when(processed.firstTime(any(), any())).thenReturn(true);
        return o;
    }

    private EventEnvelope<PaymentAuthorized> authorized(Order o, UUID paymentId) {
        return EventEnvelope.of("PaymentAuthorized", o.getId().toString(), "corr-1", "cause-1",
                new PaymentAuthorized(o.getId(), paymentId));
    }

    @Test
    @SuppressWarnings("unchecked")
    void paymentAuthorizedAfterCancellationRequestsAVoid() {
        Order o = order();
        o.cancel("payment timeout");
        UUID paymentId = UUID.randomUUID();
        var in = authorized(o, paymentId);

        handler.onPaymentAuthorized(in);

        ArgumentCaptor<EventEnvelope<?>> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(outbox).record(eq(Topics.PAYMENT_VOID_REQUESTED), captor.capture());
        EventEnvelope<PaymentVoidRequested> out = (EventEnvelope<PaymentVoidRequested>) captor.getValue();
        assertEquals("PaymentVoidRequested", out.type());
        assertEquals(o.getId().toString(), out.aggregateId());
        assertEquals("corr-1", out.correlationId());
        assertEquals(in.eventId().toString(), out.causationId());
        assertEquals(o.getId(), out.payload().orderId());
        assertEquals(paymentId, out.payload().paymentId());
        assertEquals(new BigDecimal("7.00"), out.payload().amount());
        assertEquals("BRL", out.payload().currency());
        assertEquals(OrderStatus.CANCELLED, o.getStatus());
        verify(processed).firstTime(in.eventId(), "order-service");
    }

    @Test
    void duplicateDeliveryDoesNotRequestASecondVoid() {
        Order o = order();
        o.cancel("x");
        when(processed.firstTime(any(), any())).thenReturn(false);

        handler.onPaymentAuthorized(authorized(o, UUID.randomUUID()));

        verifyNoInteractions(outbox);
    }

    @Test
    void confirmedAndRefundedOrdersAreNeverVoided() {
        Order o = order();
        o.confirm();
        handler.onPaymentAuthorized(authorized(o, UUID.randomUUID()));
        assertEquals(OrderStatus.CONFIRMED, o.getStatus());

        o.refund(new BigDecimal("7"));
        handler.onPaymentAuthorized(authorized(o, UUID.randomUUID()));
        assertEquals(OrderStatus.REFUNDED, o.getStatus());
        verify(outbox, never()).record(eq(Topics.PAYMENT_VOID_REQUESTED), any());
    }
}
