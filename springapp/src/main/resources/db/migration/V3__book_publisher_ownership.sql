-- V3: Add publisher ownership to book table
-- A book now tracks which user (publisher/admin) created it.
-- nullable=true intentionally: existing books don't have a publisher — they keep null.
-- Application code enforces ownership for new books going forward.
ALTER TABLE book
    ADD COLUMN publisher_id BIGINT NULL,
    ADD CONSTRAINT fk_book_publisher FOREIGN KEY (publisher_id) REFERENCES users(id) ON DELETE SET NULL;
