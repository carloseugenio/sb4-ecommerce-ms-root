package br.com.cepp.ecommerce.order.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.cepp.ecommerce.order.Order;
import br.com.cepp.ecommerce.order.OrderItem;
import br.com.cepp.ecommerce.order.OrderRepository;
import br.com.cepp.ecommerce.order.OrderStatus;
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

class OrderRefundServiceTest {

    private final OrderRepository orders = mock(OrderRepository.class);
    private final IdempotencyStore idempotency = mock(IdempotencyStore.class);
    private final OutboxRecorder outbox = mock(OutboxRecorder.class);
    private final OrderService service = new OrderService(orders, idempotency, new OrderEvents(outbox),
            JsonMapper.builder().build());

    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();
    private final UUID c = UUID.randomUUID();

    private Order order(boolean confirm) {
        Order o = Order.place(UUID.randomUUID(), "BRL", List.of(
                new OrderItem(a, "A", 1, new BigDecimal("10.00")),
                new OrderItem(b, "B", 1, new BigDecimal("10.00")),
                new OrderItem(c, "C", 1, new BigDecimal("10.00"))));
        if (confirm) {
            o.confirm();
        }
        when(orders.findById(o.getId())).thenReturn(Optional.of(o));
        return o;
    }

    @SuppressWarnings("unchecked")
    private EventEnvelope<OrderRefunded> recorded() {
        ArgumentCaptor<EventEnvelope<?>> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(outbox).record(eq(Topics.ORDER_REFUNDED), captor.capture());
        return (EventEnvelope<OrderRefunded>) captor.getValue();
    }

    @Test
    void topicNames() {
        assertEquals("order.refunded.v1", Topics.ORDER_REFUNDED);
        assertEquals("order.payment-void-requested.v1", Topics.PAYMENT_VOID_REQUESTED);
    }

    @Test
    void partialRefundRecordsTheEventWithTheAllocatedLines() {
        Order o = order(true);

        Order result = service.refund(o.getId(), new BigDecimal("10"));

        assertEquals(o, result);
        EventEnvelope<OrderRefunded> e = recorded();
        assertEquals("OrderRefunded", e.type());
        assertEquals(o.getId().toString(), e.aggregateId());
        assertEquals(o.getId().toString(), e.correlationId());
        assertNull(e.causationId());
        OrderRefunded p = e.payload();
        assertEquals(o.getId(), p.orderId());
        assertEquals(new BigDecimal("10.00"), p.amount());
        assertEquals(new BigDecimal("10.00"), p.totalRefunded());
        assertEquals("BRL", p.currency());
        assertEquals(false, p.full());
        assertEquals(3, p.lines().size());
        assertEquals(a, p.lines().get(0).productId());
        assertEquals(new BigDecimal("3.34"), p.lines().get(0).amount());
        assertEquals(new BigDecimal("3.33"), p.lines().get(1).amount());
        assertEquals(new BigDecimal("3.33"), p.lines().get(2).amount());
    }

    @Test
    void finalRefundIsFullCumulativeAndSkipsLinesWithNothingLeft() {
        Order o = order(true);
        o.refund(new BigDecimal("20.00"));
        // remaining per line is now 3.33 / 3.34 / 3.33 after the first split of 20.00: 6.67, 6.67, 6.66 refunded
        service.refund(o.getId(), new BigDecimal("10.00"));

        OrderRefunded p = recorded().payload();
        assertTrue(p.full());
        assertEquals(new BigDecimal("30.00"), p.totalRefunded());
        assertEquals(new BigDecimal("10.00"), p.amount());
        assertEquals(OrderStatus.REFUNDED, o.getStatus());
        assertEquals(new BigDecimal("10.00"),
                p.lines().stream().map(l -> l.amount()).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Test
    void linesWithZeroShareAreOmitted() {
        Order o = Order.place(UUID.randomUUID(), "BRL", List.of(
                new OrderItem(a, "A", 1, new BigDecimal("0.01")),
                new OrderItem(b, "B", 1, new BigDecimal("99.99"))));
        o.confirm();
        when(orders.findById(o.getId())).thenReturn(Optional.of(o));

        service.refund(o.getId(), new BigDecimal("1.00"));

        OrderRefunded p = recorded().payload();
        assertEquals(1, p.lines().size());
        assertEquals(b, p.lines().get(0).productId());
        assertEquals(new BigDecimal("1.00"), p.lines().get(0).amount());
    }

    @Test
    void failedRefundsRecordNothing() {
        Order pending = order(false);
        assertThrows(IllegalStateException.class, () -> service.refund(pending.getId(), new BigDecimal("1.00")));
        Order confirmed = order(true);
        assertThrows(IllegalArgumentException.class, () -> service.refund(confirmed.getId(), new BigDecimal("30.01")));
        UUID unknown = UUID.randomUUID();
        when(orders.findById(unknown)).thenReturn(Optional.empty());
        assertThrows(OrderNotFoundException.class, () -> service.refund(unknown, new BigDecimal("1.00")));
        verifyNoInteractions(outbox);
    }

    private String savedHash(String key, UUID orderId) {
        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(idempotency).save(eq(key), hash.capture(), eq(orderId));
        return hash.getValue();
    }

    @Test
    void keyedRefundSavesTheKeyAndRecordsTheEventOnce() {
        Order o = order(true);
        when(idempotency.find("k")).thenReturn(Optional.empty());

        service.refund(o.getId(), new BigDecimal("10.00"), "k");

        savedHash("k", o.getId());
        verify(outbox, times(1)).record(eq(Topics.ORDER_REFUNDED), any());
    }

    @Test
    void repeatedKeyReplaysWithoutRefundingAgainEvenForNumericallyEqualAmounts() {
        Order o = order(true);
        when(idempotency.find("k")).thenReturn(Optional.empty());
        service.refund(o.getId(), new BigDecimal("10.00"), "k");
        String hash = savedHash("k", o.getId());
        when(idempotency.find("k")).thenReturn(Optional.of(new IdempotencyStore.Stored(hash, o.getId())));

        Order again = service.refund(o.getId(), new BigDecimal("10"), "k");

        assertEquals(o, again);
        assertEquals(new BigDecimal("10.00"), o.getRefundedAmount());
        verify(outbox, times(1)).record(eq(Topics.ORDER_REFUNDED), any());
        verify(idempotency, times(1)).save(anyString(), anyString(), any(UUID.class));
    }

    @Test
    void replayAfterTheOrderBecameRefundedStillSucceeds() {
        Order o = order(true);
        when(idempotency.find("k")).thenReturn(Optional.empty());
        service.refund(o.getId(), new BigDecimal("30.00"), "k");
        String hash = savedHash("k", o.getId());
        assertEquals(OrderStatus.REFUNDED, o.getStatus());
        when(idempotency.find("k")).thenReturn(Optional.of(new IdempotencyStore.Stored(hash, o.getId())));

        assertEquals(o, service.refund(o.getId(), new BigDecimal("30.00"), "k"));
        verify(outbox, times(1)).record(eq(Topics.ORDER_REFUNDED), any());
    }

    @Test
    void keyReusedForADifferentRequestIsAConflict() {
        Order o = order(true);
        when(idempotency.find("k")).thenReturn(Optional.empty());
        service.refund(o.getId(), new BigDecimal("10.00"), "k");
        String hash = savedHash("k", o.getId());
        when(idempotency.find("k")).thenReturn(Optional.of(new IdempotencyStore.Stored(hash, o.getId())));

        assertThrows(IdempotencyConflictException.class, () -> service.refund(o.getId(), new BigDecimal("10.01"), "k"));
        Order other = order(true);
        assertThrows(IdempotencyConflictException.class, () -> service.refund(other.getId(), new BigDecimal("10.00"), "k"));
        when(idempotency.find("placed")).thenReturn(Optional.of(new IdempotencyStore.Stored("place-hash", o.getId())));
        assertThrows(IdempotencyConflictException.class, () -> service.refund(o.getId(), new BigDecimal("10.00"), "placed"));
        assertEquals(new BigDecimal("10.00"), o.getRefundedAmount());
    }

    @Test
    void failedKeyedRefundDoesNotConsumeTheKey() {
        Order pending = order(false);
        when(idempotency.find("k")).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, () -> service.refund(pending.getId(), new BigDecimal("1.00"), "k"));
        Order confirmed = order(true);
        assertThrows(IllegalArgumentException.class, () -> service.refund(confirmed.getId(), new BigDecimal("31.00"), "k"));
        verify(idempotency, never()).save(anyString(), anyString(), any(UUID.class));
        verifyNoInteractions(outbox);
    }

    private record MirrorItem(UUID productId, String productName, int quantity, BigDecimal unitPrice) {
    }

    private record MirrorCommand(UUID customerId, String currency, List<MirrorItem> items) {
    }

    @Test
    void placeRequestHashIsTheSameAsBeforeRefundsExisted() throws Exception {
        var json = JsonMapper.builder().build();
        UUID customer = UUID.randomUUID();
        var item = new OrderItem(a, "Keyboard", 2, new BigDecimal("10.50"));
        when(idempotency.find("p")).thenReturn(Optional.empty());
        when(orders.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        Order placed = service.place("p", new PlaceOrderCommand(customer, "BRL", List.of(item)));

        String expected = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(
                json.writeValueAsString(new MirrorCommand(customer, "BRL",
                        List.of(new MirrorItem(a, "Keyboard", 2, new BigDecimal("10.50")))))
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertEquals(expected, savedHash("p", placed.getId()));
    }
}