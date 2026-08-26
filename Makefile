SHELL := /bin/bash

.PHONY: dev build test lint
dev:
	docker compose up --build
build:
	mvn -B -T1C verify
	cd web && npm ci && npm run lint && npm run build
test:
	mvn -B -T1C test
lint:
	cd web && npm ci && npm run lint
