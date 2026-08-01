#!/usr/bin/env bash
set -euo pipefail

# Visual output helpers
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}==> Checking build prerequisites...${NC}"

# 1. Verify Docker Daemon Availability (Crucial for Testcontainers)
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}ERROR: Docker daemon is not running! Testcontainers requires Docker.${NC}"
    exit 1
fi

COMPOSE_FILE="docker-compose.test.yml"
# Cleanup hook: Always stop containers when script exits (even if build fails)
cleanup() {
    echo -e "${GREEN}==> Stopping test infrastructure...${NC}"
    docker compose -f "$COMPOSE_FILE" down --volumes --remove-orphans || true
}
trap cleanup EXIT

echo -e "${GREEN}==> Spinning up test infrastructure (Redis & Postgres)...${NC}"
docker compose -f "$COMPOSE_FILE" up -d --wait

echo -e "${GREEN}==> Cleaning and building Gradle modules...${NC}"
./gradlew clean build jacocoTestReport --no-daemon

echo -e "${GREEN}==> Build & Test Suite completed successfully!${NC}"