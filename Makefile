
all: run 

run: db-up
	@echo "Starting Spring Boot application..."
	@./mvnw spring-boot:run

db-up:
	@echo "Starting MongoDB container..."
	@docker compose up -d
db-down:
	@echo "Stopping MongoDB container..."
	@docker compose down

db-restart:
	@echo "Restarting MongoDB container..."
	@docker compose down && docker-compose up -d

logs:
	@echo "Streaming MongoDB logs..."
	@docker compose logs -f

status:
	@echo "Checking container status..."
	@docker compose ps

.PHONY: all run clean db-up db-down db-restart logs status