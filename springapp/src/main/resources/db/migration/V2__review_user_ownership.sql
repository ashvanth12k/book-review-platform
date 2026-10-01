-- V2: Enforce review ownership constraints
-- Step 1: Remove any existing reviews with null user_id (orphaned/anonymous reviews)
DELETE FROM review WHERE user_id IS NULL;

-- Step 2: Add unique constraint (one review per user per book)
ALTER TABLE review
    ADD CONSTRAINT uq_review_user_book UNIQUE (book_id, user_id);

-- Step 3: Make user_id non-nullable — every review must have an owner
ALTER TABLE review
    MODIFY COLUMN user_id BIGINT NOT NULL;

-- Step 4: Add FK constraint linking review.user_id → users.id
ALTER TABLE review
    ADD CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Step 5: Add FK constraint linking review.book_id → book.id
ALTER TABLE review
    ADD CONSTRAINT fk_review_book FOREIGN KEY (book_id) REFERENCES book(id) ON DELETE CASCADE;
