include .env

DB_CONNECTION_STRING := postgres://$(DB_USER):$(DB_PASSWORD)@$(DB_HOST):$(DB_PORT)/$(DB_NAME)?sslmode=$(DB_SSL_MODE)

migrate-up:
	migrate -path database_migrations -database "$(DB_CONNECTION_STRING)" up

migrate-down:
	migrate -path database_migrations -database "$(DB_CONNECTION_STRING)" down

migrate-up-one:
	migrate -path database_migrations -database "$(DB_CONNECTION_STRING)" up 1

migrate-down-one:
	migrate -path database_migrations -database "$(DB_CONNECTION_STRING)" down 1

.PHONY: migrate-up migrate-down migrate-up-one migrate-down-one

