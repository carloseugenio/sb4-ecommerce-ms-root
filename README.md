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

## Directory Structure

````text
sb4-ecommerce-ms-root/
├── platform-libs/          ## Shared libraries
│ ├── common-domain/        ## Domain models, DTOs
│ ├── common-security/      ## Security utilities, JWT handlers
│ ├── common-messaging/     ## Kafka/SQS abstractions
│ └── common-persistence/   ## JPA entities, base repositories
│ └── common-spring/        ## Other Spring Dependencies
├── services/               ## Deployable microservices
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

