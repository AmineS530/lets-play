all: run

run: db-up
	@echo "Starting Spring Boot application..."
	@cd ./backend && ./mvnw spring-boot:run

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

.PHONY: all run db-up db-down db-restart logs status
