package br.com.cepp.ecommerce.order.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cepp.ecommerce.order.Order;
import br.com.cepp.ecommerce.order.OrderItem;
import br.com.cepp.ecommerce.order.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class OrderServiceTest {

    private final OrderRepository orders = mock(OrderRepository.class);
    private final IdempotencyStore idempotency = mock(IdempotencyStore.class);
    private final OrderEvents events = mock(OrderEvents.class);
    private final JsonMapper json = JsonMapper.builder().build();
    private final OrderService service = new OrderService(orders, idempotency, events, json);

    private final PlaceOrderCommand command = new PlaceOrderCommand(UUID.randomUUID(), "BRL",
            List.of(new OrderItem(UUID.randomUUID(), "Keyboard", 1, new BigDecimal("10.00"))));

    @Test
    void firstRequestSavesOrderIdempotencyKeyAndOutboxEvent() {
        when(idempotency.find("k1")).thenReturn(Optional.empty());
        when(orders.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        Order order = service.place("k1", command);

        verify(idempotency).save(anyString(), anyString(), any(UUID.class));
        verify(events).placed(order, "k1");
    }

    @Test
    void sameKeyAndPayloadReplaysExistingOrderWithoutNewEvent() {
        Order existing = Order.place(command.customerId(), command.currency(), command.items());
        String hash = captureHash();
        org.mockito.Mockito.clearInvocations(orders, events);
        when(idempotency.find("k1")).thenReturn(Optional.of(new IdempotencyStore.Stored(hash, existing.getId())));
        when(orders.findById(existing.getId())).thenReturn(Optional.of(existing));

        assertEquals(existing, service.place("k1", command));
        verify(orders, never()).save(any());
        verify(events, never()).placed(any(), anyString());
    }

    @Test
    void sameKeyWithDifferentPayloadIsRejected() {
        when(idempotency.find("k1")).thenReturn(Optional.of(new IdempotencyStore.Stored("other-hash", UUID.randomUUID())));

        assertThrows(IdempotencyConflictException.class, () -> service.place("k1", command));
    }

    private String captureHash() {
        var captured = new String[1];
        when(idempotency.find("probe")).thenReturn(Optional.empty());
        when(orders.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        service.place("probe", command);
        org.mockito.ArgumentCaptor<String> c = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(idempotency).save(anyString(), c.capture(), any(UUID.class));
        captured[0] = c.getValue();
        return captured[0];
    }
}