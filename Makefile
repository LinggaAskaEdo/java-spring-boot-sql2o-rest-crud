.PHONY: clean build run test db-create db-drop db-migrate db-reset db-status db-clean

MAVEN := mvn
APP_NAME := java-spring-boot-sql2o-rest-crud
JAR_FILE := target/$(APP_NAME)-0.0.1.jar
FLYWAY_VERSION := 12.1.1
FLYWAY_PLUGIN := org.flywaydb:flyway-maven-plugin:$(FLYWAY_VERSION)
FLYWAY_MIGRATION_DIR := src/main/resources/db/migration
MYSQL_DOCKER_HOST ?= localhost
MYSQL_DOCKER_PORT ?= 3306
MYSQL_DOCKER_DATABASE ?= $(APP_NAME)
MYSQL_DOCKER_USERNAME ?= root
MYSQL_DOCKER_PASSWORD ?= 

help: ## Show this help message
	@grep -E '^[a-zA-Z_-]+:.*##' $(MAKEFILE_LIST) \
		| awk 'BEGIN {FS = ":.*##"}; {printf "  \033[36m%-25s\033[0m %s\n", $$1, $$2}'

clean: ## Clean service and logs
	@mvn clean
	@rm -rf logs/

build:clean ## Build service (skip tests)
	@mvn package -DskipTests

run: build ## Build and run service
	@java -jar $(JAR_FILE)

test: ## Run all tests
	@mvn test

gen-migration: ## Generate migration (usage: make gen-migration desc=your_desc)
	@desc="${desc}"; \
	if [ -z "$$desc" ]; then \
		echo "Usage: make gen-migration desc=your_description"; \
		exit 1; \
	fi; \
	VERSION=$$(date +%Y%m%d%H%M%S); \
	FILENAME="$(FLYWAY_MIGRATION_DIR)/V$${VERSION}__$${desc}.sql"; \
	mkdir -p "$(FLYWAY_MIGRATION_DIR)"; \
	echo "-- Migration: $$desc" > "$$FILENAME"; \
	echo "-- Created : $$(date -u '+%Y-%m-%d %H:%M:%S UTC')" >> "$$FILENAME"; \
	echo "" >> "$$FILENAME"; \
	echo "Created: $$FILENAME"

db-migrate: ## Run Flyway migrations
	@echo "Running Flyway migrations..."
	@$(MAVEN) $(FLYWAY_PLUGIN):migrate \
			-Dflyway.url="jdbc:mysql://$(MYSQL_DOCKER_HOST):$(MYSQL_DOCKER_PORT)/$(MYSQL_DOCKER_DATABASE)?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
			-Dflyway.user=$(MYSQL_DOCKER_USERNAME) \
			-Dflyway.password="$(MYSQL_DOCKER_PASSWORD)"

db-info: ## Show Flyway migration status
	@echo "Showing migration info..."
	@$(MAVEN) $(FLYWAY_PLUGIN):info \
			-Dflyway.url="jdbc:mysql://$(MYSQL_DOCKER_HOST):$(MYSQL_DOCKER_PORT)/$(MYSQL_DOCKER_DATABASE)?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
			-Dflyway.user=$(MYSQL_DOCKER_USERNAME) \
			-Dflyway.password="$(MYSQL_DOCKER_PASSWORD)"

db-repair: ## Repair Flyway checksum mismatches
	@echo "Repairing migrations..."
	@$(MAVEN) $(FLYWAY_PLUGIN):repair \
			-Dflyway.url="jdbc:mysql://$(MYSQL_DOCKER_HOST):$(MYSQL_DOCKER_PORT)/$(MYSQL_DOCKER_DATABASE)?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
			-Dflyway.user=$(MYSQL_DOCKER_USERNAME) \
			-Dflyway.password="$(MYSQL_DOCKER_PASSWORD)"

db-clean: ## DROP all objects in commerce-core schema (dev only)
	@printf "WARNING: This will drop ALL objects in $(MYSQL_DOCKER_DATABASE). Continue? [y/N] "; read confirm; \
	if [ "$$confirm" != "y" ] && [ "$$confirm" != "Y" ]; then \
		echo "Cancelled."; exit 0; \
	fi; \
	echo "Dropping all objects..."; \
	$(MAVEN) $(FLYWAY_PLUGIN):clean \
			-Dflyway.url="jdbc:mysql://$(MYSQL_DOCKER_HOST):$(MYSQL_DOCKER_PORT)/$(MYSQL_DOCKER_DATABASE)?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
			-Dflyway.user=$(MYSQL_DOCKER_USERNAME) \
			-Dflyway.password="$(MYSQL_DOCKER_PASSWORD)"