# Variables
SERVICE = cross-border
VERSION ?= latest
IMAGE_NAME = $(SERVICE):$(VERSION)
HEALTH_URL = http://localhost:8082/actuator/health
MAX_RETRIES = 30
RETRY_INTERVAL = 2
NODE_CMD = node mockFxRateSender.js http://localhost:8082/fx-rate
NODE_LOG = node.log
PID_FILE = node.pid
# Gradle build and Docker commands
.PHONY: build
build:
	./gradlew clean build
	docker build --build-arg SERVICE=$(SERVICE) -t $(IMAGE_NAME) .

# Docker Compose commands
.PHONY: up
up:
	docker-compose up -d
	$(call check_health)
	@echo "All services are up and healthy!"

	@echo "Starting node fxrate daemon..."
	@nohup $(NODE_CMD) > $(NODE_LOG) 2>&1 & echo $$! > $(PID_FILE)
	@echo "Node daemon started with PID $$(cat $(PID_FILE))"
	@echo "Logs available in $(NODE_LOG)"

.PHONY: down
down:
	docker-compose down

.PHONY: logs
logs:
	docker-compose logs -f

# Combined commands
.PHONY: deploy
deploy: build up

# Clean up commands
.PHONY: clean
clean:
	docker-compose down -v
	docker rmi $(IMAGE_NAME)

# Helper commands
.PHONY: ps
ps:
	docker-compose ps


# Health check function
define check_health
	@for i in `seq 1 $(MAX_RETRIES)`; do \
		echo "Checking health ($$i/$(MAX_RETRIES))..."; \
		if curl -s $(HEALTH_URL) | grep -q '"status":"UP"'; then \
			echo "Service is healthy!"; \
			exit 0; \
		fi; \
		echo "Service not healthy yet, waiting $(RETRY_INTERVAL) seconds..."; \
		sleep $(RETRY_INTERVAL); \
	done; \
	echo "Service failed to become healthy after $$(( $(MAX_RETRIES) * $(RETRY_INTERVAL) )) seconds"; \
	exit 1
endef

.PHONY: help
help:
	@echo "Available commands:"
	@echo "  make build    - Build the gradle project and create Docker image"
	@echo "  make up       - Start all services"
	@echo "  make down     - Stop all services"
	@echo "  make logs     - View logs from all services"
	@echo "  make deploy   - Build and start all services"
	@echo "  make clean    - Remove containers, volumes, and images"
	@echo "  make ps       - List running services"
	@echo "  make help     - Show this help message"