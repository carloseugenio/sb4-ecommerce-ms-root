package br.com.cepp.ecommerce.order.application;

import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class IdempotencyStore {

    public record Stored(String requestHash, UUID orderId) {
    }

    private final JdbcTemplate jdbc;

    public IdempotencyStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Stored> find(String key) {
        return jdbc.query("SELECT request_hash, order_id FROM idempotency_key WHERE key_value = ?",
                (rs, i) -> new Stored(rs.getString("request_hash"), rs.getObject("order_id", UUID.class)), key)
                .stream().findFirst();
    }

    /** Throws DuplicateKeyException if a concurrent request already stored the same key. */
    public void save(String key, String requestHash, UUID orderId) {
        jdbc.update("INSERT INTO idempotency_key (key_value, request_hash, order_id, created_at) VALUES (?, ?, ?, now())",
                key, requestHash, orderId);
    }
}