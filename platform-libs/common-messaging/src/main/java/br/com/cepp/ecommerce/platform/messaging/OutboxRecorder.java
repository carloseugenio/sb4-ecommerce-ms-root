package br.com.cepp.ecommerce.platform.messaging;

import br.com.cepp.ecommerce.platform.domain.event.EventEnvelope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Writes events to the outbox table. Must be called inside the business transaction. */
public class OutboxRecorder {

    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    public OutboxRecorder(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String topic, EventEnvelope<?> event) {
        jdbc.update("""
                INSERT INTO outbox_event (event_id, topic, aggregate_id, type, payload, created_at)
                VALUES (?, ?, ?, ?, ?, now())
                """,
                event.eventId(), topic, event.aggregateId(), event.type(), json.writeValueAsString(event));
    }
}