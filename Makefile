all: run

run: db-up
	@echo "Starting Spring Boot application..."
	@cd ./backend && ./mvnw spring-boot:run

build:
	@echo "Building application..."
	@cd ./backend && ./mvnw clean package -DskipTests

clean:
	@echo "Cleaning build artifacts..."
	@cd ./backend && ./mvnw clean

db-up:
	@echo "Starting MongoDB container..."
	@docker compose up -d

db-down:
	@echo "Stopping MongoDB container..."
	@docker compose down

db-restart:
	@echo "Restarting MongoDB container..."
	@docker compose restart

logs:
	@echo "Streaming Docker logs..."
	@docker compose logs -f

status:
	@echo "Checking container status..."
	@docker compose ps

help:
	@echo "Let's Play Makefile Commands:"
	@echo "  make run          - Start Spring Boot application locally"
	@echo "  make build        - Compile and package JAR artifact"
	@echo "  make clean        - Remove build artifacts"
	@echo "  make db-up        - Start MongoDB container"
	@echo "  make db-down      - Stop MongoDB container"
	@echo "  make db-restart   - Restart MongoDB container"
	@echo "  make logs         - Stream container logs"
	@echo "  make status       - Check Docker container status"

.PHONY: all run build clean db-up db-down db-restart logs status help
