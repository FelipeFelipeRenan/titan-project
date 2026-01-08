# Titan Project - Distributed Financial Ledger System

## Overview

Titan Project is a high-performance, distributed ledger system designed for financial transactions with strong consistency, event-driven architecture, and comprehensive observability. Built with microservices architecture using Java Spring Boot and Go, it provides robust account management, transfer capabilities, and transaction tracking.

## Architecture

### Core Services

- **titan-ledger-core**: Java Spring Boot service handling core ledger operations including account management, transfers, and deposits
- **titan-outbox-worker**: Go service for processing outbox events ensuring eventual consistency

### Infrastructure Components

- **PostgreSQL**: Primary data store with ACID transactions
- **Redis**: Caching layer and idempotency key storage
- **Kafka**: Message broker for event-driven communication
- **Jaeger**: Distributed tracing for request flow analysis
- **Prometheus & Grafana**: Metrics collection and visualization
- **Kafka UI**: Message broker monitoring and management

## Features

- ✅ **Account Management**: Create and manage financial accounts
- ✅ **Peer-to-Peer Transfers**: Secure inter-account transfers with idempotency
- ✅ **Deposits**: Cash-in functionality for account funding
- ✅ **Transaction History**: Complete statement and ledger tracking
- ✅ **Idempotency**: Safe retry mechanisms with Redis and PostgreSQL
- ✅ **Event-Driven Architecture**: Outbox pattern for eventual consistency
- ✅ **Observability**: Full-stack monitoring with tracing and metrics
- ✅ **Race Condition Prevention**: Pessimistic locking for data integrity
- ✅ **Caching**: Redis-based caching for performance optimization
- ✅ **API Documentation**: Swagger/OpenAPI integration

## Prerequisites

- Docker and Docker Compose
- Java 21 (for development)
- Go 1.25+ (for development)

## Getting Started

### Running the System

1. Clone the repository:
```bash
git clone <repository-url>
cd titan-project
```

2. Start all services using Docker Compose:
```bash
cd infrastructure/docker
docker-compose up --build
```

3. Access the services:
   - **Ledger Core API**: `http://localhost:8081`
   - **Swagger UI**: `http://localhost:8081/swagger-ui.html`
   - **Kafka UI**: `http://localhost:8080`
   - **Jaeger UI**: `http://localhost:16686`
   - **Prometheus**: `http://localhost:9090`
   - **Grafana**: `http://localhost:3000` (admin/admin)

### API Endpoints

#### Account Management
- `GET /api/v1/accounts` - List all accounts
- `POST /api/v1/accounts` - Create a new account
- `GET /api/v1/accounts/{accountId}` - Get account balance and status
- `GET /api/v1/accounts/{accountId}/statement` - Get account statement (paginated)

#### Transactions
- `POST /api/v1/accounts/{accountId}/deposit` - Deposit funds
- `POST /api/v1/accounts/transfer` - Transfer funds between accounts (requires `Idempotency-Key` header)

### Idempotency

All transfer operations require an `Idempotency-Key` header to ensure safe retries:

```bash
curl -X POST "http://localhost:8081/api/v1/accounts/transfer" \
  -H "Idempotency-Key: unique-key-123" \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccountId": "123e4567-e89b-12d3-a456-426614174000",
    "toAccountId": "123e4567-e89b-12d3-a456-426614174001",
    "amount": 100.00,
    "description": "Transfer description"
  }'
```

## System Design

### Key Patterns Implemented

1. **Outbox Pattern**: Ensures reliable event publishing by storing events in the same transaction as business operations
2. **CQRS**: Separation of read and write operations for optimized performance
3. **Event-Driven Architecture**: Loose coupling between services using Kafka
4. **Pessimistic Locking**: Prevents race conditions during financial operations
5. **Idempotency**: Safe retry mechanisms using both Redis and PostgreSQL
6. **Distributed Caching**: Redis for frequently accessed data

### Data Flow

1. Client sends transfer request to titan-ledger-core
2. Service validates and processes the transfer in a transaction
3. Transaction and ledger entries are created
4. Event is stored in outbox table within the same transaction
5. titan-outbox-worker polls the outbox and publishes events to Kafka
6. Events can be consumed by other services for further processing

## Testing

The project includes comprehensive testing:
- Unit tests for business logic
- Integration tests using Testcontainers
- Race condition testing script (`race-condition-test.js`)

## Monitoring & Observability

- **Metrics**: Micrometer with Prometheus integration
- **Tracing**: OpenTelemetry with Jaeger
- **Health Checks**: Spring Boot Actuator endpoints
- **Logging**: Structured logging with SLF4J

## Security Considerations

- Environment variables for sensitive configuration
- Idempotency keys for safe retries
- Transactional operations to maintain data consistency
- Input validation (can be enhanced)

## Development

### Project Structure

```
/workspace/
├── infrastructure/
│   └── docker/                 # Docker Compose and configuration
├── services/
│   ├── titan-ledger-core/      # Java Spring Boot service
│   └── titan-outbox-worker/    # Go outbox worker
```

### Building Services

**For titan-ledger-core:**
```bash
cd services/titan-ledger-core
./mvnw clean package
```

**For titan-outbox-worker:**
```bash
cd services/titan-outbox-worker
go build -o app cmd/main.go
```

## Roadmap

### Planned Improvements
- Authentication and authorization system
- Enhanced error handling and circuit breakers
- Advanced validation and rate limiting
- Multi-currency support
- Batch processing capabilities
- Advanced reporting and analytics

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

[Specify license type here]

## Support

For support, please open an issue in the GitHub repository.