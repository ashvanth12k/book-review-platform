-- V1: Initial schema — captures the full existing table structure
-- This migration runs against an empty (or baselined) database.
-- baseline-on-migrate=true means Flyway will mark existing DBs as already at V0
-- and apply all V1+ migrations going forward.

CREATE TABLE IF NOT EXISTS users (
    id       BIGINT       NOT NULL AUTO_INCREMENT,
    name     VARCHAR(255),
    email    VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role     VARCHAR(50)  NOT NULL DEFAULT 'ROLE_USER',
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS book (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    title       VARCHAR(255),
    author      VARCHAR(255),
    genre       VARCHAR(255),
    description TEXT,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS review (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    book_id     BIGINT NOT NULL,
    user_id     BIGINT,
    review_text TEXT,
    rating      INT    NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
