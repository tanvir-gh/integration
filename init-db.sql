-- Create schemas for each microservice
CREATE SCHEMA IF NOT EXISTS content_db;
CREATE SCHEMA IF NOT EXISTS recommendation_db;

-- Grant permissions to postgres user
GRANT ALL PRIVILEGES ON SCHEMA content_db TO postgres;
GRANT ALL PRIVILEGES ON SCHEMA recommendation_db TO postgres;

-- Set default privileges for future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA content_db GRANT ALL ON TABLES TO postgres;
ALTER DEFAULT PRIVILEGES IN SCHEMA recommendation_db GRANT ALL ON TABLES TO postgres;
