# ADR-0002: Checkout consistency with choreography saga, transactional outbox and idempotency

Status: Accepted (2026-11-02)

## Decision
- Checkout is a choreography saga: OrderPlaced -> StockReserved -> PaymentAuthorized -> OrderConfirmed. Failures publish StockRejected / PaymentFailed and trigger compensation (release stock, cancel order, refund).
- Every state change that emits an event writes it to an `outbox_event` table in the same local transaction; a poller (`FOR UPDATE SKIP LOCKED`) publishes to Kafka at-least-once, keyed by aggregateId.
- Consumers are idempotent via a `processed_message` table keyed by eventId. HTTP commands use an `Idempotency-Key` header with a stored response. Payment provider calls use provider idempotency keys.
- Events: versioned envelope (eventId, type, version, aggregateId, occurredAt, correlationId, causationId, payload); topics `<domain>.<event>.v1`, DLT `<topic>.dlt`.
- Order states: PENDING -> CONFIRMED | CANCELLED, transitions enforced in the aggregate; timeout job cancels stale PENDING orders.

## Consequences
No distributed transactions; duplicates and reordering must be tolerated; end-to-end saga tests required for each failure path.

## Alternatives
Orchestrated saga (central coordinator), 2PC (rejected), CDC with Debezium (possible later if polling lag is a problem).
