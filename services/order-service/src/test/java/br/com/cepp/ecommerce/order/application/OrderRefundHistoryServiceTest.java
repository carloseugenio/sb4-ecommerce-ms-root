package br.com.cepp.ecommerce.order.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.cepp.ecommerce.order.Order;
import br.com.cepp.ecommerce.order.OrderItem;
import br.com.cepp.ecommerce.order.OrderRepository;
import br.com.cepp.ecommerce.order.OrderStatus;
import br.com.cepp.ecommerce.platform.domain.event.CheckoutEvents.OrderRefundReversed;
import br.com.cepp.ecommerce.platform.domain.event.CheckoutEvents.OrderRefunded;
import br.com.cepp.ecommerce.platform.domain.event.EventEnvelope;
import br.com.cepp.ecommerce.platform.domain.event.Topics;
import br.com.cepp.ecommerce.platform.messaging.OutboxRecorder;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

class OrderRefundHistoryServiceTest {

    private final OrderRepository orders = mock(OrderRepository.class);
    private final IdempotencyStore idempotency = mock(IdempotencyStore.class);
    private final OutboxRecorder outbox = mock(OutboxRecorder.class);
    private final OrderService service = new OrderService(orders, idempotency, new OrderEvents(outbox),
            JsonMapper.builder().build());

    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();

    private Order order() {
        Order o = Order.place(UUID.randomUUID(), "BRL", List.of(
                new OrderItem(a, "A", 1, new BigDecimal("10.00")),
                new OrderItem(b, "B", 1, new BigDecimal("10.00"))));
        o.confirm();
        when(orders.findById(o.getId())).thenReturn(Optional.of(o));
        return o;
    }

    @SuppressWarnings("unchecked")
    private <T> EventEnvelope<T> recorded(String topic) {
        ArgumentCaptor<EventEnvelope<?>> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(outbox).record(eq(topic), captor.capture());
        return (EventEnvelope<T>) captor.getValue();
    }

    @Test
    void topicName() {
        assertEquals("order.refund-reversed.v1", Topics.ORDER_REFUND_REVERSED);
    }

    @Test
    void undoRecordsARefundReversedEvent() {
        Order o = order();
        o.refund(new BigDecimal("15.00"));
        when(orders.findById(o.getId())).thenReturn(Optional.of(o));

        Order result = service.undoRefund(o.getId(), null);

        assertEquals(o, result);
        EventEnvelope<OrderRefundReversed> e = recorded(Topics.ORDER_REFUND_REVERSED);
        assertEquals("OrderRefundReversed", e.type());
        assertEquals(o.getId().toString(), e.aggregateId());
        assertEquals(o.getId().toString(), e.correlationId());
        OrderRefundReversed p = e.payload();
        assertEquals(new BigDecimal("15.00"), p.amount());
        assertEquals(new BigDecimal("0.00"), p.totalRefunded());
        assertEquals("BRL", p.currency());
        assertEquals(2, p.lines().size());
        assertEquals(a, p.lines().get(0).productId());
        assertEquals(new BigDecimal("7.50"), p.lines().get(0).amount());
    }

    @Test
    void redoRecordsAnOrderRefundedEventWithTheOriginalShares() {
        Order o = order();
        o.refund(new BigDecimal("20.00"));
        o.undoRefund();

        service.redoRefund(o.getId(), null);

        EventEnvelope<OrderRefunded> e = recorded(Topics.ORDER_REFUNDED);
        assertEquals(new BigDecimal("20.00"), e.payload().amount());
        assertEquals(new BigDecimal("20.00"), e.payload().totalRefunded());
        assertEquals(true, e.payload().full());
        assertEquals(OrderStatus.REFUNDED, o.getStatus());
    }

    @Test
    void failedUndoRecordsNothingAndKeepsTheKey() {
        Order o = order();
        when(idempotency.find("k")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.undoRefund(o.getId(), "k"));
        assertThrows(IllegalStateException.class, () -> service.redoRefund(o.getId(), "k"));
        assertThrows(OrderNotFoundException.class, () -> {
            UUID unknown = UUID.randomUUID();
            when(orders.findById(unknown)).thenReturn(Optional.empty());
            service.undoRefund(unknown, "k");
        });

        verify(idempotency, never()).save(anyString(), anyString(), any(UUID.class));
        verifyNoInteractions(outbox);
    }

    @Test
    void repeatedUndoKeyReplaysWithoutUndoingAgain() {
        Order o = order();
        o.refund(new BigDecimal("5.00"));
        o.refund(new BigDecimal("5.00"));
        when(idempotency.find("k")).thenReturn(Optional.empty());
        service.undoRefund(o.getId(), "k");
        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(idempotency).save(eq("k"), hash.capture(), eq(o.getId()));
        when(idempotency.find("k")).thenReturn(Optional.of(new IdempotencyStore.Stored(hash.getValue(), o.getId())));

        assertEquals(o, service.undoRefund(o.getId(), "k"));

        assertEquals(new BigDecimal("5.00"), o.getRefundedAmount());
        verify(outbox, times(1)).record(eq(Topics.ORDER_REFUND_REVERSED), any());
    }

    @Test
    void aKeyIsNotInterchangeableBetweenUndoRedoAndRefund() {
        Order o = order();
        o.refund(new BigDecimal("5.00"));
        when(idempotency.find("k")).thenReturn(Optional.empty());
        service.undoRefund(o.getId(), "k");
        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(idempotency).save(eq("k"), hash.capture(), eq(o.getId()));
        when(idempotency.find("k")).thenReturn(Optional.of(new IdempotencyStore.Stored(hash.getValue(), o.getId())));

        assertThrows(IdempotencyConflictException.class, () -> service.redoRefund(o.getId(), "k"));
        assertThrows(IdempotencyConflictException.class,
                () -> service.refund(o.getId(), new BigDecimal("5.00"), "k"));
        Order other = order();
        assertThrows(IdempotencyConflictException.class, () -> service.undoRefund(other.getId(), "k"));
    }
}
