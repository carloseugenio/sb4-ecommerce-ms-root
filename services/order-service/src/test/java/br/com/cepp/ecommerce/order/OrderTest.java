package br.com.cepp.ecommerce.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderTest {

    private static Order newOrder() {
        return Order.place(UUID.randomUUID(), "BRL",
                List.of(new OrderItem(UUID.randomUUID(), "Keyboard", 2, new BigDecimal("10.50")),
                        new OrderItem(UUID.randomUUID(), "Mouse", 1, new BigDecimal("5.00"))));
    }

    @Test
    void placedOrderIsPendingWithComputedTotal() {
        Order order = newOrder();

        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(new BigDecimal("26.00"), order.getTotalAmount());
    }

    @Test
    void confirmChangesPendingOnlyOnce() {
        Order order = newOrder();

        assertTrue(order.confirm());
        assertFalse(order.confirm());
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }

    @Test
    void cancelledOrderIgnoresLaterConfirmation() {
        Order order = newOrder();

        assertTrue(order.cancel("PAYMENT_FAILED"));
        assertFalse(order.confirm());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals("PAYMENT_FAILED", order.getCancelReason());
    }

    @Test
    void confirmedOrderCannotBeCancelledBySagaEvents() {
        Order order = newOrder();
        order.confirm();

        assertFalse(order.cancel("OUT_OF_STOCK"));
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }

    @Test
    void rejectsEmptyOrAnInvalidItem() {
        assertThrows(IllegalArgumentException.class, () -> Order.place(UUID.randomUUID(), "BRL", List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new OrderItem(UUID.randomUUID(), "X", 0, BigDecimal.ONE));
    }
}