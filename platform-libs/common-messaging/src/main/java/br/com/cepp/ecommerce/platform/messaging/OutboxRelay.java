package br.com.cepp.ecommerce.platform.messaging;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

/** Polls unpublished outbox rows and sends them to Kafka (at-least-once, keyed by aggregateId). */
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private record Row(java.util.UUID eventId, String topic, String aggregateId, String payload) {
    }

    private final JdbcTemplate jdbc;
    private final KafkaTemplate<String, String> kafka;
    private final int batchSize;

    public OutboxRelay(JdbcTemplate jdbc, KafkaTemplate<String, String> kafka, int batchSize) {
        this.jdbc = jdbc;
        this.kafka = kafka;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "")
    @Transactional
    public void publishPending() {
        List<Row> rows = jdbc.query("""
                SELECT event_id, topic, aggregate_id, payload FROM outbox_event
                WHERE published_at IS NULL ORDER BY created_at LIMIT ? FOR UPDATE SKIP LOCKED
                """,
                (rs, i) -> new Row(rs.getObject("event_id", java.util.UUID.class), rs.getString("topic"),
                        rs.getString("aggregate_id"), rs.getString("payload")),
                batchSize);
        for (Row row : rows) {
            try {
                kafka.send(row.topic(), row.aggregateId(), row.payload()).get(10, TimeUnit.SECONDS);
                jdbc.update("UPDATE outbox_event SET published_at = now() WHERE event_id = ?", row.eventId());
            } catch (Exception e) {
                // Stop the batch to keep per-aggregate ordering; the rest stays pending for the next poll.
                log.warn("Outbox publish failed for event {}: {}", row.eventId(), e.toString());
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                return;
            }
        }
    }
}