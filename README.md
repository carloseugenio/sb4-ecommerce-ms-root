# Spring-Boot Cloud Ecommerce application with microservices

## Description

This is an example of a Spring-Boot microservices project for a Ecommerce application.

### Build

The build system used in this project is Gradle and in this example we are using the Monorepo approach
 ([Gradle Multi-Project Build](https://docs.gradle.org/current/userguide/multi_project_builds.html)) for simplicity,
 clarity and maintainability with a single Git Repository.

If a complete separate Git repositories layout is needed, then consider changing to the Multi-Repo Approach via
[Gradle Composite Builds](https://docs.gradle.org/current/userguide/composite_builds.html).

Some project layout and build decisions were inspired on this article: 
[Spring Boot Monorepo on AWS](https://levelup.gitconnected.com/stop-juggling-microservices-the-spring-boot-monorepo-blueprint-for-aws-6e8705e1d0f4). 

## Architecture Patterns

We use [**Amazon AWS Prescriptive Guidance**](https://docs.aws.amazon.com/prescriptive-guidance/latest/cloud-design-patterns/introduction.html) for this project please refer to it to understand 
the pattersn and their usage.

## Domain Driver Design Architecture Usage Overview

Nevertheless, some strong Domain Driven Design Concepts about microservices bound contexts and separations are in 
place:

### Inside the Domain Service (Private)Domain Entities & Aggregates: 
 
- Database models, state transitions, and business invariants (e.g., Order, Customer, Invoice).
- Domain Services: Business logic coordinating multiple aggregates that does not fit naturally into a single entity.
- Repository Interfaces: Explicit contracts defining how data is stored locally.

### Inside the Shared Library (Public)

- Data Transfer Objects (DTOs): Plain data schemas or API contracts (like ProtoBuf files or JSON wrappers) used 
strictly for inter-service communication.
- Cross-Cutting Utilities: Technical infrastructure such as logger wrappers, JWT token parsers, 
error-handling middleware, or generic database drivers.
- Value Objects (Optional): Highly primitive, behavior-less wrappers like a Money object or a Currency enum, 
provided they have no business rules attached.

### How Services Should Interact Without Sharing Classes

Instead of coupling services together using shared domain models, 
use decoupled integration patterns:

- **Expose Explicit Contracts**: Services should only interact via explicit 
APIs (REST, gRPC) or asynchronous messaging (Kafka, RabbitMQ).
- **Translate Incoming Data**: When a service consumes a contract from another service, 
it should translate that payload into its own internal domain model using an 
[**Anti-Corruption Layer (ACL)**](https://docs.aws.amazon.com/prescriptive-guidance/latest/cloud-design-patterns/acl.html).
- **Reference by ID Only**: An aggregate in one microservice should never reference 
an entity object from another microservice. It must strictly reference it by its Primary Key / ID 
value (e.g., storing customerId: "123" instead of a Customer object).

## Directory Structure

````text
sb4-ecommerce-ms-root/
├── platform-libs/          ## Shared libraries
│ ├── common-domain/        ## DTOs, value objects like money or value enums, mappers, generic errors 
│ ├── common-security/      ## Security utilities, JWT handlers
│ ├── common-messaging/     ## Kafka/SQS abstractions
│ └── common-persistence/   ## generic database drivers, base repositories
│ └── common-spring/        ## Other Spring Dependencies
├── services/               ## Deployable microservices with domain, persistence, APIs and messaging
│ ├── customer-service/
│ ├── order-service/
│ │ ├── src/
│ │ ├── Dockerfile
│ │ └── build.gradle
│ ├── inventory-service/
│ ├── payment-service/
│ └── notification-service/
├── infrastructure/         ## IaC and deployment configs
│ ├── terraform/
│ │ ├── modules/
│ │ ├── environments/
│ │ └── service-configs/
│ ├── kubernetes/
│ │ └── manifests/
│ └── docker-compose.yml    ## Local development
├── gradle/
│ └── libs.versions.toml    ## Centralized dependency versions
├── settings.gradle
├── build.gradle            ## Root build configuration
└── README.md
````

