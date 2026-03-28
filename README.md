# Rate Limiter Service

This is a Spring Boot application that implements an in-memory Rate Limiter using the **Token Bucket Algorithm**.

## Features

- **Token Bucket Algorithm**: Smoothly rate-limits requests by refilling tokens progressively over time while allowing bursts up to the bucket's capacity.
- **Thread-safe**: Utilizes `ConcurrentHashMap` and `synchronized` blocks for safe concurrent request processing.
- **Per-User Limits**: Rate limits are applied independently to different users (identified by a `User-Id` header).

## Getting Started

### Prerequisites

- Java Development Kit (JDK)
- Maven

### Running the Application

1. Clone the repository and navigate to the project directory:
   ```bash
   cd ratelimiter
   ```
2. Run the application using the Maven wrapper:
   ```bash
   ./mvnw spring-boot:run
   ```
   *(For Windows Command Prompt, use `mvnw.cmd spring-boot:run`)*

## Endpoints

### Test Resource
A sample REST endpoint is provided to test the rate limiter behavior.

**GET** `/api/resource`

**Headers:**
- `User-Id`: A unique identifier for the user (optional, defaults to `defaultUser`).

**Responses:**
- `200 OK`: If the request is allowed (a token was successfully consumed).
  - Response Body: `Request successful! Resource accessed.`
- `429 Too Many Requests`: If the user has exceeded their token capacity (no tokens available).
  - Response Body: `Too many requests! Rate limit exceeded.`

## Configuration
By default, the rate limiter is configured with the following constraints in `RateLimiterService.java`:
- **Bucket Capacity**: 5 tokens maximum
- **Refill Rate**: 5 tokens per 60 seconds (60,000 milliseconds)

Tokens are refilled continuously as time passes, allowing requests to incrementally recover tokens.
