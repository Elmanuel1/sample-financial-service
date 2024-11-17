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
- Gradle 8.x
- Docker and Docker Compose
- PostgreSQL 15 (if running locally)
- Node.js (for auxiliary services)

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
