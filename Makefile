# Makefile for Spring Boot Docker Application

# Load environment variables from .env file
ifneq (,$(wildcard ./.env))
    include .env
    export
endif

# Variables
IMAGE_NAME := invoices-store
IMAGE_TAG := local
CONTAINER_NAME := invoices-test
LOCAL_PORT := 8080
CONTAINER_PORT := 8080

# Default target
.PHONY: help
help: ## Show this help message
	@echo "Available targets:"
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  %-20s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

# Build targets
.PHONY: clean
clean: ## Clean build artifacts and Docker containers/images
	@echo "🧹 Cleaning up..."
	./gradlew clean
	-docker stop $(CONTAINER_NAME) 2>/dev/null
	-docker rm $(CONTAINER_NAME) 2>/dev/null
	-docker rmi $(IMAGE_NAME):$(IMAGE_TAG) 2>/dev/null

.PHONY: build-jar
build-jar: ## Build the Spring Boot JAR
	@echo "🔨 Building JAR..."
	./gradlew bootJar
	@echo "📦 JAR files created:"
	@ls -la build/libs/

.PHONY: build-image
build-image: build-jar ## Build Docker image with secrets from .env
	@echo "🐳 Building Docker image..."
	@if [ -z $(JWT_SECRET) ] || [ -z $(MONGODB_CONNECTION_STRING) ]; then \
		echo "❌ Error: JWT_SECRET and MONGODB_CONNECTION_STRING must be set in .env file"; \
		exit 1; \
	fi
	docker build \
		--build-arg JWT_SECRET=$(JWT_SECRET) \
		--build-arg MONGODB_CONNECTION_STRING=$(MONGODB_CONNECTION_STRING) \
		-t $(IMAGE_NAME):$(IMAGE_TAG) \
		.
	@echo "✅ Docker image built successfully!"

.PHONY: run
run: ## Run the Docker container
	@echo "🚀 Starting container..."
	-docker stop $(CONTAINER_NAME) 2>/dev/null
	-docker rm $(CONTAINER_NAME) 2>/dev/null
	docker run -d \
		-p $(LOCAL_PORT):$(CONTAINER_PORT) \
		--name $(CONTAINER_NAME) \
		$(IMAGE_NAME):$(IMAGE_TAG)
	@echo "✅ Container started on http://localhost:$(LOCAL_PORT)"

.PHONY: run-foreground
run-foreground: ## Run the Docker container in foreground (see logs)
	@echo "🚀 Starting container in foreground..."
	-docker stop $(CONTAINER_NAME) 2>/dev/null
	-docker rm $(CONTAINER_NAME) 2>/dev/null
	docker run \
		-p $(LOCAL_PORT):$(CONTAINER_PORT) \
		--name $(CONTAINER_NAME) \
		$(IMAGE_NAME):$(IMAGE_TAG)

.PHONY: logs
logs: ## Show container logs
	@echo "📋 Container logs:"
	docker logs $(CONTAINER_NAME)

.PHONY: logs-follow
logs-follow: ## Follow container logs
	@echo "📋 Following container logs (Ctrl+C to stop):"
	docker logs -f $(CONTAINER_NAME)

.PHONY: shell
shell: ## Get a shell inside the running container
	@echo "🐚 Opening shell in container..."
	docker exec -it $(CONTAINER_NAME) /bin/bash

.PHONY: inspect
inspect: ## Inspect the container (debug info)
	@echo "🔍 Container inspection:"
	@echo "Files in /app:"
	docker exec $(CONTAINER_NAME) ls -la /app/
	@echo "\nEnvironment variables:"
	docker exec $(CONTAINER_NAME) env | grep -E "(JWT|MONGODB)" | sed 's/=.*/=***REDACTED***/'
	@echo "\nJava version:"
	docker exec $(CONTAINER_NAME) java -version

# Test targets
.PHONY: test-health
test-health: ## Test the health endpoint
	@echo "🏥 Testing health endpoint..."
	@sleep 2
	curl -f http://localhost:$(LOCAL_PORT)/actuator/health || echo "❌ Health check failed"

.PHONY: test-auth
test-auth: ## Test the auth endpoint (will fail without valid user)
	@echo "🔐 Testing auth endpoint..."
	curl -X POST http://localhost:$(LOCAL_PORT)/api/auth/login \
		-H "Content-Type: application/json" \
		-d '{"email":"test@example.com","password":"testpass"}' || echo "Expected to fail without valid user"

.PHONY: status
status: ## Show container status
	@echo "📊 Container status:"
	@docker ps --filter name=$(CONTAINER_NAME) --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

.PHONY: stop
stop: ## Stop the container
	@echo "🛑 Stopping container..."
	docker stop $(CONTAINER_NAME)
	@echo "✅ Container stopped"

.PHONY: restart
restart: stop run ## Restart the container
	@echo "🔄 Container restarted"

# Combined targets
.PHONY: dev
dev: clean build-image run logs-follow ## Full development cycle: clean, build, run and follow logs

.PHONY: quick-test
quick-test: build-image run ## Quick test: build and run, then test endpoints
	@echo "⏳ Waiting for application to start..."
	@sleep 10
	@make test-health
	@make status

# Deployment helpers
.PHONY: check-env
check-env: ## Check if .env file exists and has required variables
	@echo "🔍 Checking environment setup..."
	@if [ ! -f .env ]; then \
		echo "❌ .env file not found. Please create one with:"; \
		echo "JWT_SECRET=your_base64_jwt_secret"; \
		echo "MONGODB_CONNECTION_STRING=your_mongodb_connection_string"; \
		exit 1; \
	fi
	@if [ -z "$(JWT_SECRET)" ]; then echo "❌ JWT_SECRET not set in .env"; exit 1; fi
	@if [ -z "$(MONGODB_CONNECTION_STRING)" ]; then echo "❌ MONGODB_CONNECTION_STRING not set in .env"; exit 1; fi
	@echo "✅ Environment variables are set"

.PHONY: docker-info
docker-info: ## Show Docker image information
	@echo "🐳 Docker image info:"
	docker images $(IMAGE_NAME):$(IMAGE_TAG)
	@echo "\n📝 Image layers:"
	docker history $(IMAGE_NAME):$(IMAGE_TAG)