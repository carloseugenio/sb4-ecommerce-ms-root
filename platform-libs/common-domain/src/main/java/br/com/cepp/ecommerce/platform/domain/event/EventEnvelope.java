package br.com.cepp.ecommerce.platform.domain.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Versioned wrapper for every event published to Kafka. Payload is a JSON-serializable record. */
public record EventEnvelope<T>(
        UUID eventId,
        String type,
        int version,
        String aggregateId,
        Instant occurredAt,
        String correlationId,
        String causationId,
        T payload) {

    public EventEnvelope {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(aggregateId, "aggregateId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(payload, "payload");
        if (version < 1) {
            throw new IllegalArgumentException("version must be >= 1");
        }
    }

    public static <T> EventEnvelope<T> of(String type, String aggregateId, String correlationId,
                                          String causationId, T payload) {
        return new EventEnvelope<>(UUID.randomUUID(), type, 1, aggregateId, Instant.now(),
                correlationId, causationId, payload);
    }
}