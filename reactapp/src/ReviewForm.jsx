import React from 'react';

export default function ReviewForm({
  books = [],
  selectedBookId,
  reviewForm,
  onBookSelect,
  onChange,
  onSubmit,
  submitLabel = 'Add Review',
}) {
  return (
    <form onSubmit={onSubmit} className="review-form">
      <select
        name="bookId"
        value={selectedBookId || ''}
        onChange={(e) => onBookSelect(e.target.value)}
      >
        <option value="">Select book</option>
        {books.map((book) => (
          <option key={book.id} value={book.id}>
            {book.title}
          </option>
        ))}
      </select>

      <input
        type="text"
        name="comment"
        placeholder="Comment"
        value={reviewForm.comment}
        onChange={onChange}
      />

      <input
        type="number"
        name="rating"
        placeholder="Rating (1–5)"
        value={reviewForm.rating}
        onChange={onChange}
        min="1"
        max="5"
      />

      <button type="submit" className="btn">{submitLabel}</button>
    </form>
  );
}
