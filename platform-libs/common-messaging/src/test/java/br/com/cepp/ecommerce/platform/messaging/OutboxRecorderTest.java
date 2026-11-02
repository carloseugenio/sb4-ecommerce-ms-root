package br.com.cepp.ecommerce.platform.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import br.com.cepp.ecommerce.platform.domain.event.CheckoutEvents.OrderConfirmed;
import br.com.cepp.ecommerce.platform.domain.event.EventEnvelope;
import br.com.cepp.ecommerce.platform.domain.event.Topics;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;

class OutboxRecorderTest {

    @Test
    void insertsSerializedEnvelopeIntoOutbox() {
        var jdbc = mock(JdbcTemplate.class);
        var recorder = new OutboxRecorder(jdbc, JsonMapper.builder().build());
        var orderId = UUID.randomUUID();
        var event = EventEnvelope.of("OrderConfirmed", orderId.toString(), "corr", null, new OrderConfirmed(orderId));

        recorder.record(Topics.ORDER_CONFIRMED, event);

        verify(jdbc).update(contains("INSERT INTO outbox_event"), eq(event.eventId()), eq(Topics.ORDER_CONFIRMED),
                eq(orderId.toString()), eq("OrderConfirmed"), contains(orderId.toString()));
    }
}