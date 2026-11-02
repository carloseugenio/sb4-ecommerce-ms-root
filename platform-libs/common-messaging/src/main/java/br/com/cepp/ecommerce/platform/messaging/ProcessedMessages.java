package br.com.cepp.ecommerce.platform.messaging;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Idempotent consumer helper: call inside the handler transaction and skip the handler if false. */
public class ProcessedMessages {

    private final JdbcTemplate jdbc;

    public ProcessedMessages(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public boolean firstTime(UUID eventId, String consumer) {
        return jdbc.update("""
                INSERT INTO processed_message (event_id, consumer, processed_at) VALUES (?, ?, now())
                ON CONFLICT (event_id, consumer) DO NOTHING
                """, eventId, consumer) == 1;
    }
}