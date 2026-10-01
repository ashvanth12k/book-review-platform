import React from 'react';
import { canManageBook, canDeleteBook } from './api';

function renderStars(rating) {
  if (rating === undefined || rating === null || rating === '') return null;
  const r = parseFloat(rating);
  if (isNaN(r)) return null;
  const fullStars   = Math.floor(r);
  const hasHalfStar = r % 1 >= 0.5;
  let stars = '';
  for (let i = 0; i < fullStars; i++) stars += '⭐';
  if (hasHalfStar && fullStars < 5) stars += '½';
  return stars;
}

export default function BookList({ books = [], currentUser, onEditBook, onDeleteBook }) {
  const showActions = !!(onEditBook || onDeleteBook);
  return (
    <div>
      <table className="books-table">
        <thead>
          <tr>
            <th>Title</th>
            <th>Author</th>
            <th>Genre</th>
            <th>Description</th>
            <th>Average Rating</th>
            {showActions && <th>Actions</th>}
          </tr>
        </thead>
        <tbody>
          {books.length === 0 ? (
            <tr>
              <td colSpan={showActions ? 6 : 5} className="empty-state">
                No books available
              </td>
            </tr>
          ) : (
            books.map((book) => (
              <tr key={book.id}>
                <td>{book.title}</td>
                <td>{book.author}</td>
                <td>{book.genre}</td>
                <td>{book.description}</td>
                <td>
                  {book.averageRating === undefined || book.averageRating === null || book.averageRating === '' ? (
                    <span className="no-reviews">No reviews yet</span>
                  ) : (
                    <div className="rating-display">
                      <span className="stars">{renderStars(book.averageRating)}</span>
                      <span className="rating-value">({book.averageRating})</span>
                    </div>
                  )}
                </td>
                {showActions && (
                  <td>
                    <div className="book-actions">
                      {onEditBook && canManageBook(currentUser, book) && (
                        <button
                          className="btn btn--sm"
                          onClick={() => onEditBook(book)}
                          title="Edit book"
                        >
                          Edit
                        </button>
                      )}
                      {onDeleteBook && canDeleteBook(currentUser, book) && (
                        <button
                          className="btn btn--sm btn--danger"
                          onClick={() => onDeleteBook(book.id)}
                          title="Delete book"
                        >
                          Delete
                        </button>
                      )}
                    </div>
                  </td>
                )}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}
