-- Runs once, when the postgres volume is created. The services' automated tests use this database, so they never
-- share data (customers, outbox events, products) with the applications running in compose.
CREATE DATABASE rpe_test;
