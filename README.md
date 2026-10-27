# E-commerce microservices

This repository is the training project I developed during my Java internship at Innowise.

The project started as a smaller Spring Boot application and was gradually split into separate services. The final version includes authentication, user, order and payment services behind an API gateway, plus the supporting infrastructure needed to run them locally.

## What is inside

- **auth-service** — registration, login, JWT authentication and role-based access
- **user-service** — user profile and payment card data
- **order-service** — items and order management
- **payment-service** — payment processing
- **api-gateway** — entry point and request authentication

Order and payment flows communicate through Kafka. Redis is used by the application for caching, while PostgreSQL is used as the main persistent storage.

## Tech stack

Java 21, Spring Boot, Spring Security, Spring Data JPA, PostgreSQL, Redis, Kafka, Gradle, Docker, Testcontainers, GitHub Actions, SonarQube and Kubernetes.

The repository also contains basic monitoring/tracing configuration with Prometheus, Loki and Tempo.

## Local run

Create the local environment file:

```bash
cp .env.example .env
```

Then start the application and its dependencies:

```bash
docker compose up --build
```

The gateway is exposed on port `8080`.

Individual services use these ports:

- auth-service — `8081`
- user-service — `8082`
- order-service — `8083`
- payment-service — `8084`

## Tests and CI

The project contains unit and integration tests. Integration tests use Testcontainers for external dependencies such as PostgreSQL and Kafka.

GitHub Actions builds and tests the services, runs SonarQube analysis and builds Docker images.

## Kubernetes

The `k8s/` directory contains manifests for the services and infrastructure used during the internship project. They were written as part of the learning process rather than as a production-ready Kubernetes setup.

## Project context

This was an individual internship project developed under mentor review. The pull request history is intentionally kept in the repository because it shows how the project evolved and how review feedback was incorporated.
