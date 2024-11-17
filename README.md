# Cross-Border Payment Service

This service handles cross-border payment processing with configurable margin rates and settlement processing.

## Table of Contents
- [Prerequisites](#prerequisites)
- [Project Structure](#project-structure)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
  - [Method 1: Docker Deployment](#method-1-docker-deployment)
- [Database Migrations](#database-migrations)
- [Health Checks](#health-checks)
- [Available Commands](#available-commands)

## Prerequisites

- JDK 21
- Docker and Docker Compose
- Node (for currency services)

## Project Structure

```
.
├── Dockerfile
├── Makefile
├── docker-compose.yml
├── spherelab.env
├── flyway/
│   └── migrations/
├── gradle/
└── src/
```

## Configuration

### Environment Variables

The application can be configured using `spherelab.env` file. Key configurations:

```properties
# App Configuration
APP_MARGIN_RATE_USD=0.02
APP_MARGIN_RATE_EUR=0.03
APP_MARGIN_RATE_GBP=0.04
APP_MARGIN_RATE_JPY=0.05
APP_MARGIN_RATE_AUD=0.06

APP_SETTLEMENT_POLL_SIZE=50
APP_MAX_SETTLEMENT_ATTEMPTS=3

# Database Configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/spherelab
SPRING_DATASOURCE_USERNAME=spherelab
SPRING_DATASOURCE_PASSWORD=spherelab
```

## Running the Application

### Method 1: Docker Deployment

1. **Build and Start Services**
   ```bash
   make deploy
   ```
   This will:
   - Build the Gradle project
   - Create Docker image
   - Start all services
   - Run database migrations
   - Start the node daemon
   - Verify health checks

2. **Check Application Status**
   ```bash
   make ps          # Check running containers
   make health      # Check application health
   make node-status # Check node daemon status
   ```

3. **View Logs**
   ```bash
   make logs      # View all container logs
   make node-logs # View node daemon logs
   ```

4. **Stop Services**
   ```bash
   make down
   ```

5. **Clean Up**
   ```bash
   make clean
   ```


## Database Migrations

Migrations are handled by Flyway and located in the `flyway/` directory.

- Docker deployment: migrations are copied to `/app/flyway`

## Health Checks

The application exposes health endpoints via Spring Boot Actuator:

- Health Check URL: `http://localhost:8082/actuator/health`
- Detailed health information is enabled

## Available Commands

### Makefile Commands
```bash
make build        # Build the gradle project and create Docker image
make up           # Start services and node daemon
make down         # Stop all services including node daemon
make logs         # View docker-compose logs
make node-logs    # View node daemon logs
make node-status  # Check node daemon status
make deploy       # Build and start all services
make clean        # Remove containers, volumes, images, and logs
make ps           # List running services
make health       # Check service health status
```

### Gradle Commands
```bash
./gradlew clean           # Clean build directories
./gradlew build          # Build the project
./gradlew bootRun        # Run the application
./gradlew test           # Run tests
./gradlew bootJar        # Create executable jar
```
## API Documentation
```text
POST http://localhost:8082/transfer
```
Sample
```bash
curl --location 'http://localhost:8080/transfer' \
--header 'Content-Type: application/json' \
--header 'Cookie: csrf_token_f1de9e489ba88cb15968b97f40f59e8ef0da5ca03ad1f37fc13a2aa45a2512a9=1XcHp5mzcc/UoD8niGimPrD8Awg/11H2UIguyu5nWNw=; csrf_token_f960fc983a2bc719627550cc2cb3977e78dabb01d739a8430f1b5263a2c5e440=7A0NUxsCrdma7n4cRV2hd2DWEsCsiNSGMzG5AWJ5SbE=' \
--data '{
    "sender_account": "1112345678956yu",
    "reference": "26oV3w0890w909",
    "amount": 498.00,
    "receiver_account": "2389456789",
    "to_currency": "GBP",
    "description": "Lead",
    "from_currency": "USD"
}'
```