all: run

run:
	@echo "Starting Spring Boot application..."
	@cd ./backend && (./mvnw spring-boot:run || mvn spring-boot:run)

test:
	@echo "Running tests..."
	@cd ./backend && (./mvnw test || mvn test)

build:
	@echo "Building application..."
	@cd ./backend && (./mvnw clean package -DskipTests || mvn clean package -DskipTests)

clean:
	@echo "Cleaning build artifacts..."
	@cd ./backend && (./mvnw clean || mvn clean)

compose-up:
	@echo "Starting Docker Compose stack..."
	@docker compose up -d

compose-down:
	@echo "Stopping Docker Compose stack..."
	@docker compose down

compose-logs:
	@echo "Following Docker Compose logs..."
	@docker compose logs -f

db-up:
	@echo "Starting MongoDB container..."
	@docker compose up -d mongodb

db-down:
	@echo "Stopping MongoDB container..."
	@docker compose stop mongodb

db-restart:
	@echo "Restarting MongoDB container..."
	@docker compose restart mongodb

logs:
	@echo "Streaming Docker logs..."
	@docker compose logs -f

status:
	@echo "Checking container status..."
	@docker compose ps

help:
	@echo "Let's Play Makefile Commands:"
	@echo "  make run          - Start Spring Boot application locally"
	@echo "  make test         - Run unit & integration test suite"
	@echo "  make build        - Compile and package JAR artifact"
	@echo "  make clean        - Remove build artifacts"
	@echo "  make compose-up   - Start full backend & MongoDB via Docker Compose"
	@echo "  make compose-down - Stop Docker Compose stack"
	@echo "  make compose-logs - Follow Docker Compose logs"
	@echo "  make db-up        - Start MongoDB container"
	@echo "  make db-down      - Stop MongoDB container"
	@echo "  make status       - Check Docker container status"

.PHONY: all run test build clean compose-up compose-down compose-logs db-up db-down db-restart logs status help
