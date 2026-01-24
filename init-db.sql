-- Create schemas for each microservice
CREATE SCHEMA IF NOT EXISTS catalog_db;
CREATE SCHEMA IF NOT EXISTS watch_history_db;

-- Grant permissions to postgres user
GRANT ALL PRIVILEGES ON SCHEMA catalog_db TO postgres;
GRANT ALL PRIVILEGES ON SCHEMA watch_history_db TO postgres;

-- Set default privileges for future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA catalog_db GRANT ALL ON TABLES TO postgres;
ALTER DEFAULT PRIVILEGES IN SCHEMA watch_history_db GRANT ALL ON TABLES TO postgres;
