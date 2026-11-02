# ADR-0001: Architecture style, messaging and data ownership

Status: Accepted (2026-11-02)

## Context
Ecommerce platform on Spring Boot 4.1 / Gradle monorepo, deployed on AWS. Requirements evolve incrementally; keep the design simple.

## Decision
- Microservices per bounded context (customer, catalog, inventory, cart, order, payment, notification) behind a Spring Cloud API Gateway.
- Asynchronous event-driven integration over Kafka (local container, Amazon MSK in stg/prod). Synchronous REST only where a query is needed, wrapped with Resilience4j.
- Database-per-service (PostgreSQL + Liquibase). No shared tables, joins or foreign keys across services; reference by ID only; ACL when consuming events.
- Coding style: SIMPLE (YAGNI, records, constructor injection, package-by-feature).

## Consequences
Eventual consistency between services; need idempotency and compensation (ADR-0002); more operational infrastructure (Kafka, per-service DB).

## Alternatives
SNS/SQS (cheaper, weaker ordering/replay), RabbitMQ, shared database (rejected: coupling).
