package br.com.cepp.ecommerce.platform.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    @Test
    void GivenOrder_WhenCreateOrder_ThenOrderCreated() {
        Order order = new Order();
        assertNotNull(order);
        UUID id = UUID.randomUUID();
        order = Order.builder()
                .id(id)
                .status(OrderStatus.PENDING)
                .items(List.of(OrderItem.builder().build()))
                .build();
        assertNotNull(order);
        assertEquals(id, order.getId());
        assertEquals(1, order.getItems().size());
    }
}