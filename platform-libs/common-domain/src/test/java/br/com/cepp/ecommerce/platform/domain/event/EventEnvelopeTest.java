package br.com.cepp.ecommerce.platform.domain.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventEnvelopeTest {

    @Test
    void ofCreatesVersionOneEnvelope() {
        var orderId = UUID.randomUUID();
        var event = EventEnvelope.of("OrderConfirmed", orderId.toString(), "corr-1", null,
                new CheckoutEvents.OrderConfirmed(orderId));

        assertEquals(1, event.version());
        assertEquals(orderId.toString(), event.aggregateId());
    }

    @Test
    void rejectsInvalidVersion() {
        assertThrows(IllegalArgumentException.class, () -> new EventEnvelope<>(UUID.randomUUID(), "t", 0,
                "a", java.time.Instant.now(), null, null, "p"));
    }

    @Test
    void buildsDeadLetterTopicName() {
        assertEquals("order.placed.v1.dlt", Topics.dlt(Topics.ORDER_PLACED));
    }
}