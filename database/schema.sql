CREATE DATABASE IF NOT EXISTS electricitydb;
USE electricitydb;

-- Table for consumers
CREATE TABLE IF NOT EXISTS consumer (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(200),
    email VARCHAR(100),
    meterNumber VARCHAR(30),
    phone VARCHAR(30)
);

-- Table for bills
CREATE TABLE IF NOT EXISTS bill (
    id INT AUTO_INCREMENT PRIMARY KEY,
    consumer_id INT NOT NULL,
    units DOUBLE,
    amount DOUBLE,
    billing_date DATE,
    due_date DATE,
    CONSTRAINT fk_consumer FOREIGN KEY (consumer_id) REFERENCES consumer(id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Table for admins (for authenticated deletes)
CREATE TABLE IF NOT EXISTS admin (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(128) NOT NULL
);

-- Seed a default admin (username: admin, password: admin123)
-- SHA-256("admin123") = 240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9
INSERT IGNORE INTO admin(username, password_hash)
VALUES ('admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9');

-- Additional admins
INSERT IGNORE INTO admin(username, password_hash)
VALUES ('tanay', SHA2('240205', 256));

INSERT IGNORE INTO admin(username, password_hash)
VALUES ('sakshi', SHA2('081105', 256));

-- Table for complaints
CREATE TABLE IF NOT EXISTS complaint (
    id INT AUTO_INCREMENT PRIMARY KEY,
    description TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN'
);

-- Extend consumer schema: birthdate and passcode (derived from birthdate as DDMMYY)
-- Note: Some MySQL versions don't support "ADD COLUMN IF NOT EXISTS".
-- Use plain ALTERs and let the initializer ignore duplicate-column errors on re-run.
ALTER TABLE consumer
    ADD COLUMN birthdate DATE DEFAULT '1999-01-01';

ALTER TABLE consumer
    ADD COLUMN passcode VARCHAR(10) DEFAULT '010199';

-- Add phone column if missing (for older installations)
ALTER TABLE consumer
    ADD COLUMN phone VARCHAR(30);

-- Backfill passcode for existing rows where missing, using birthdate or default
UPDATE consumer
SET passcode = DATE_FORMAT(COALESCE(birthdate, '1999-01-01'), '%d%m%y')
WHERE passcode IS NULL OR passcode = '';