import React, { useEffect, useState, useMemo } from 'react';
import { Provider, useDispatch, useSelector } from 'react-redux';
import './App.css';
import BookForm from './BookForm';
import BookList from './BookList';
import ReviewForm from './ReviewForm';
import { createAppStore } from './redux/store';
import {
  createBook, createReview,
  editBook, removeBook,
  editReview, removeReview,
  fetchBooks,
} from './redux/booksSlice';
import {
  getAdminStats, getRecommendations,
  canEditReview, canDeleteReview,
  getPublishers, createAdminUser, getAllUsers, deleteUser,
} from './api';

// ─── Constants ────────────────────────────────────────────────────────────────
const emptyBookForm   = { title: '', author: '', genre: '', description: '', publisherEmail: '' };
const emptyReviewForm = { comment: '', rating: '' };
const emptyUserForm   = { name: '', email: '', password: '', role: 'ROLE_PUBLISHER' };

// ─── Pure helpers ─────────────────────────────────────────────────────────────
function getRoleLabel(role) {
  return { ROLE_ADMIN: 'Admin', ROLE_PUBLISHER: 'Publisher', ROLE_USER: 'User' }[role] || role;
}

function canAddBooks(userRole) {
  return userRole === 'ROLE_PUBLISHER' || userRole === 'ROLE_ADMIN';
}

/**
 * Only ROLE_USER may post or edit reviews.
 * Publishers can view reviews only.
 * Admins moderate (delete) — they do not post or edit as a reviewer.
 */
function canPostReview(userRole) {
  return userRole === 'ROLE_USER';
}

// ─── AppContent ───────────────────────────────────────────────────────────────
function AppContent({ onLogout, userRole, username, currentUser }) {
  const dispatch     = useDispatch();
  const books        = useSelector((s) => s.books.items);
  const errorMessage = useSelector((s) => s.books.error);

  // ── Book form state ──
  const [bookForm,       setBookForm]       = useState(emptyBookForm);
  const [editingBookId,  setEditingBookId]  = useState(null); // null = add mode
  const [bookSuccess,    setBookSuccess]    = useState('');
  const [bookError,      setBookError]      = useState('');

  // ── Review form state ──
  const [reviewForm,      setReviewForm]      = useState(emptyReviewForm);
  const [selectedBookId,  setSelectedBookId]  = useState('');
  const [editingReviewId, setEditingReviewId] = useState(null); // null = add mode
  const [reviewSuccess,   setReviewSuccess]   = useState('');
  const [reviewError,     setReviewError]     = useState('');

  // ── Search & sort ──
  const [searchQuery, setSearchQuery] = useState('');
  const [sortBy,      setSortBy]      = useState('default');

  // Initial load
  useEffect(() => { dispatch(fetchBooks()); }, [dispatch]);

  // ── Admin data refresh helper (stats + users + publishers all at once) ────────
  const refreshAdminData = async () => {
    const [stats, updatedUsers, updatedPublishers] = await Promise.all([
      getAdminStats(),
      getAllUsers(),
      getPublishers(),
    ]);
    setAdminStats(stats);
    setAllUsers(updatedUsers);
    setPublishers(updatedPublishers);
  };

  // ── Admin stats — loaded on mount, auto-refreshed after every user mutation ──
  const [adminStats, setAdminStats] = useState(null);
  useEffect(() => {
    if (currentUser?.role !== 'ROLE_ADMIN') { setAdminStats(null); return; }
    getAdminStats().then(setAdminStats).catch(() => setAdminStats(null));
  }, [currentUser]);

  // ── Publishers list — admin only, used for the "Add Book" dropdown ────────────
  const [publishers, setPublishers] = useState([]);
  useEffect(() => {
    if (currentUser?.role !== 'ROLE_ADMIN') { setPublishers([]); return; }
    getPublishers().then(setPublishers).catch(() => setPublishers([]));
  }, [currentUser]);

  // ── Manage Users state — admin only ──────────────────────────────────────────
  const [allUsers,      setAllUsers]      = useState([]);
  const [userForm,      setUserForm]      = useState(emptyUserForm);
  const [userSuccess,   setUserSuccess]   = useState('');
  const [userError,     setUserError]     = useState('');
  const [userSearch,    setUserSearch]    = useState('');
  const [userRoleFilter,setUserRoleFilter]= useState('ALL');

  useEffect(() => {
    if (currentUser?.role !== 'ROLE_ADMIN') { setAllUsers([]); return; }
    getAllUsers().then(setAllUsers).catch(() => setAllUsers([]));
  }, [currentUser]);

  const handleUserFormChange = (e) => {
    const { name, value } = e.target;
    setUserForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleCreateUser = async (e) => {
    e.preventDefault();
    setUserError('');
    const { name, email, password, role } = userForm;
    if (!name || !email || !password) {
      setUserError('Name, email and password are required.');
      return;
    }
    try {
      await createAdminUser({ name, email, password, role });
      setUserForm(emptyUserForm);
      setUserSuccess(`${ role === 'ROLE_PUBLISHER' ? 'Publisher' : 'User' } "${name}" created!`);
      setTimeout(() => setUserSuccess(''), 4000);
      await refreshAdminData();
    } catch (err) {
      setUserError(err?.message || 'Failed to create user.');
    }
  };

  const handleDeleteUser = async (userId) => {
    if (!window.confirm('Delete this user? This cannot be undone.')) return;
    setUserError('');
    try {
      await deleteUser(userId);
      setUserSuccess('User deleted.');
      setTimeout(() => setUserSuccess(''), 3000);
      await refreshAdminData();
    } catch (err) {
      setUserError(err?.message || 'Failed to delete user.');
    }
  };

  // Filtered + searched user list for Manage Users table
  const filteredUsers = useMemo(() => {
    const q = userSearch.trim().toLowerCase();
    return allUsers.filter((u) => {
      const matchRole = userRoleFilter === 'ALL' || u.role === userRoleFilter;
      const matchSearch = !q ||
        (u.username || u.name || '').toLowerCase().includes(q) ||
        (u.email || '').toLowerCase().includes(q);
      return matchRole && matchSearch;
    });
  }, [allUsers, userSearch, userRoleFilter]);


  // ── Derived: recommendations — async, fetched from backend (ROLE_USER only) ──
  const [recommendations, setRecommendations] = useState([]);
  useEffect(() => {
    if (currentUser?.role !== 'ROLE_USER') { setRecommendations([]); return; }
    getRecommendations()
      .then(setRecommendations)
      .catch(() => setRecommendations([]));
  }, [currentUser]);

  // ── Derived: reviews visible to the current user based on role ──────────────────
  //  • ADMIN  → all reviews (moderation access)
  //  • USER   → all reviews (browsing context)
  //  • PUBLISHER → only reviews for books they own
  const visibleReviews = useMemo(() => {
    const flat = books.flatMap((b) =>
      (b.reviews || []).map((r) => ({
        ...r,
        bookTitle:       b.title,
        bookPublisherId: b.publisherId,
      }))
    );
    if (!currentUser) return flat;
    if (currentUser.role === 'ROLE_PUBLISHER') {
      // Filter to reviews that belong to this publisher's books only
      return flat.filter((r) => r.bookPublisherId === currentUser.id);
    }
    return flat; // ADMIN and USER see all reviews
  }, [books, currentUser]);

  // ── Derived: existing review this user has already written for the selected book ──
  const existingUserReview = useMemo(() => {
    if (!selectedBookId || !currentUser || editingReviewId !== null) return null;
    const numId = parseInt(selectedBookId, 10);
    const book  = books.find((b) => b.id === numId);
    return book ? (book.reviews || []).find((r) => r.userId === currentUser.id) || null : null;
  }, [selectedBookId, books, currentUser, editingReviewId]);

  // When user picks a book they already reviewed, pre-populate the form so
  // they can see and amend their existing entry (API will upsert).
  useEffect(() => {
    if (editingReviewId !== null) return; // already in explicit edit mode
    if (!selectedBookId || !currentUser) return;
    const numId = parseInt(selectedBookId, 10);
    const book  = books.find((b) => b.id === numId);
    if (!book) return;
    const existing = (book.reviews || []).find((r) => r.userId === currentUser.id);
    if (existing) {
      setReviewForm({ comment: existing.comment, rating: String(existing.rating) });
    } else {
      setReviewForm(emptyReviewForm);
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedBookId]);

  // ═══════════════════════════ Book handlers ════════════════════════════════

  const handleBookChange = (e) => {
    const { name, value } = e.target;
    setBookForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleAddBook = async (e) => {
    e.preventDefault();
    setBookError('');
    const { title, author, genre, description, publisherEmail } = bookForm;
    if (!title || !author || !genre || !description) {
      setBookError('All fields are required.');
      return;
    }
    try {
      await dispatch(createBook({ book: { title, author, genre, description, publisherEmail }, currentUser })).unwrap();
      setBookForm(emptyBookForm);
      setBookSuccess('Book added successfully!');
      setTimeout(() => setBookSuccess(''), 3000);
    } catch (err) {
      setBookError(err?.message || 'Failed to add book.');
    }
  };

  const handleStartEditBook = (book) => {
    setEditingBookId(book.id);
    setBookForm({ title: book.title, author: book.author, genre: book.genre, description: book.description, publisherEmail: '' });
    setBookSuccess('');
    setBookError('');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleUpdateBook = async (e) => {
    e.preventDefault();
    setBookError('');
    const { title, author, genre, description } = bookForm;
    if (!title || !author || !genre || !description) {
      setBookError('All fields are required.');
      return;
    }
    try {
      await dispatch(editBook({ id: editingBookId, updates: { title, author, genre, description }, currentUser })).unwrap();
      setEditingBookId(null);
      setBookForm(emptyBookForm);
      setBookSuccess('Book updated successfully!');
      setTimeout(() => setBookSuccess(''), 3000);
    } catch (err) {
      setBookError(err?.message || 'Failed to update book.');
    }
  };

  const handleCancelEditBook = () => {
    setEditingBookId(null);
    setBookForm(emptyBookForm);
    setBookError('');
  };

  const handleDeleteBook = async (bookId) => {
    if (!window.confirm('Delete this book and all its reviews?')) return;
    try {
      await dispatch(removeBook({ id: bookId, currentUser })).unwrap();
      // If we were editing this book, exit edit mode
      if (editingBookId === bookId) { setEditingBookId(null); setBookForm(emptyBookForm); }
      setBookSuccess('Book deleted.');
      setTimeout(() => setBookSuccess(''), 3000);
    } catch (err) {
      setBookError(err?.message || 'Failed to delete book.');
    }
  };

  // ═══════════════════════════ Review handlers ══════════════════════════════

  const handleReviewChange = (e) => {
    const { name, value } = e.target;
    setReviewForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleAddReview = async (e) => {
    e.preventDefault();
    setReviewError('');
    if (!selectedBookId) { setReviewError('Please select a book to review.'); return; }
    if (!reviewForm.comment) { setReviewError('Please enter a comment.'); return; }
    const rating = parseInt(reviewForm.rating, 10);
    if (!reviewForm.rating || isNaN(rating) || rating < 1 || rating > 5) {
      setReviewError('Please enter a rating between 1 and 5.'); return;
    }
    try {
      await dispatch(createReview({ bookId: selectedBookId, review: reviewForm, currentUser })).unwrap();
      setReviewForm(emptyReviewForm);
      setSelectedBookId('');
      setReviewSuccess('Review submitted!');
      setTimeout(() => setReviewSuccess(''), 3000);
    } catch (err) {
      setReviewError(err?.message || 'Failed to add review.');
    }
  };

  const handleStartEditReview = (review) => {
    setEditingReviewId(review.id);
    setReviewForm({ comment: review.comment, rating: String(review.rating) });
    setSelectedBookId(String(review.bookId));
    setReviewSuccess('');
    setReviewError('');
  };

  const handleUpdateReview = async (e) => {
    e.preventDefault();
    setReviewError('');
    if (!reviewForm.comment) { setReviewError('Please enter a comment.'); return; }
    const rating = parseInt(reviewForm.rating, 10);
    if (!reviewForm.rating || isNaN(rating) || rating < 1 || rating > 5) {
      setReviewError('Please enter a rating between 1 and 5.'); return;
    }
    try {
      await dispatch(editReview({ reviewId: editingReviewId, updates: { ...reviewForm, bookId: selectedBookId }, currentUser })).unwrap();
      setEditingReviewId(null);
      setReviewForm(emptyReviewForm);
      setSelectedBookId('');
      setReviewSuccess('Review updated!');
      setTimeout(() => setReviewSuccess(''), 3000);
    } catch (err) {
      setReviewError(err?.message || 'Failed to update review.');
    }
  };

  const handleCancelEditReview = () => {
    setEditingReviewId(null);
    setReviewForm(emptyReviewForm);
    setSelectedBookId('');
    setReviewError('');
  };

  const handleDeleteReview = async (reviewId) => {
    if (!window.confirm('Delete this review?')) return;
    try {
      await dispatch(removeReview({ reviewId, currentUser })).unwrap();
      if (editingReviewId === reviewId) { setEditingReviewId(null); setReviewForm(emptyReviewForm); setSelectedBookId(''); }
      setReviewSuccess('Review deleted.');
      setTimeout(() => setReviewSuccess(''), 3000);
    } catch (err) {
      setReviewError(err?.message || 'Failed to delete review.');
    }
  };

  // ═══════════════════════════ Search + sort ═══════════════════════════════

  const showAddBook  = canAddBooks(userRole);
  const q            = searchQuery.trim().toLowerCase();
  const getAvg       = (b) => { const v = parseFloat(b.averageRating); return isNaN(v) ? -1 : v; };
  const getRevCount  = (b) => (Array.isArray(b.reviews) ? b.reviews.length : (b.reviewCount || 0));

  const filtered = books.filter((book) =>
    !q ||
    (book.title  || '').toLowerCase().includes(q) ||
    (book.author || '').toLowerCase().includes(q) ||
    (book.genre  || '').toLowerCase().includes(q)
  );

  const sorted = [...filtered].sort((a, b) => {
    if (sortBy === 'rating-desc') return getAvg(b) - getAvg(a);
    if (sortBy === 'rating-asc')  return getAvg(a) - getAvg(b);
    if (sortBy === 'reviews')     return getRevCount(b) - getRevCount(a);
    return 0;
  });

  // ═══════════════════════════ Render ══════════════════════════════════════

  return (
    <div className="app">

      {/* ── Header ── */}
      <header className="header">
        <h1>Book Review Platform</h1>
        {onLogout && (
          <div className="header-right">
            {username && <span className="header-username">{username}</span>}
            {userRole && (
              <span className={`role-badge role-badge--${getRoleLabel(userRole).toLowerCase()}`}>
                {getRoleLabel(userRole)}
              </span>
            )}
            <button className="btn-logout" onClick={onLogout}>Logout</button>
          </div>
        )}
      </header>

      {/* Global Redux error */}
      {errorMessage && (
        <div className="error-message" role="alert">{errorMessage}</div>
      )}

      {/* ── Dashboard Statistics (ROLE_ADMIN) ── */}
      {adminStats && (
      <div className="card">
          <h2 className="section-title">Dashboard Statistics</h2>
          <div className="stats-grid">
            <div className="stat-item">
              <span className="stat-label">Users (Reader)</span>
              <strong className="stat-value">{adminStats.totalNormalUsers ?? '—'}</strong>
            </div>
            <div className="stat-item">
              <span className="stat-label">Publishers</span>
              <strong className="stat-value">{adminStats.totalPublishers ?? '—'}</strong>
            </div>
            <div className="stat-item">
              <span className="stat-label">Total Users</span>
              <strong className="stat-value">{adminStats.totalUsers ?? '—'}</strong>
            </div>
            <div className="stat-item">
              <span className="stat-label">Total Books</span>
              <strong className="stat-value">{adminStats.totalBooks}</strong>
            </div>
          </div>
        </div>
      )}

      {/* ── Add / Edit Book ── */}
      {showAddBook && (
        <div className="card">
          <h2 className="section-title">{editingBookId ? 'Edit Book' : 'Add a Book'}</h2>
          {bookError   && <div className="error-message"   role="alert">{bookError}</div>}
          {bookSuccess && <div className="success-message" role="status">{bookSuccess}</div>}
          <BookForm
            formData={bookForm}
            onChange={handleBookChange}
            onSubmit={editingBookId ? handleUpdateBook : handleAddBook}
            submitLabel={editingBookId ? 'Update Book' : 'Add Book'}
            isAdmin={userRole === 'ROLE_ADMIN'}
            publishers={publishers}
            isEditing={!!editingBookId}
          />
          {editingBookId && (
            <button
              type="button"
              className="btn btn--secondary"
              onClick={handleCancelEditBook}
              style={{ marginTop: '0.5rem' }}
            >
              Cancel Edit
            </button>
          )}
        </div>
      )}

      {/* ── Book success/error when publisher toolbar is hidden (admins can see above) ── */}
      {!showAddBook && (bookError || bookSuccess) && (
        <div className="card">
          {bookError   && <div className="error-message"   role="alert">{bookError}</div>}
          {bookSuccess && <div className="success-message" role="status">{bookSuccess}</div>}
        </div>
      )}

      {/* ── Manage Users (ROLE_ADMIN only) ── */}
      {userRole === 'ROLE_ADMIN' && (
        <div className="card">
          <h2 className="section-title">Manage Users</h2>

          {userError   && <div className="error-message"   role="alert">{userError}</div>}
          {userSuccess && <div className="success-message" role="status">{userSuccess}</div>}

          {/* ── Add user form ── */}
          <form onSubmit={handleCreateUser} style={{ marginBottom: '1.5rem' }}>
            <h3 style={{ fontSize: '1rem', fontWeight: 600, marginBottom: '0.75rem' }}>Add User / Publisher</h3>
            <div className="form-grid">
              <div className="form-group">
                <input id="new-user-name" type="text" name="name" placeholder="Full Name"
                  value={userForm.name} onChange={handleUserFormChange} />
              </div>
              <div className="form-group">
                <input id="new-user-email" type="email" name="email" placeholder="Email"
                  value={userForm.email} onChange={handleUserFormChange} />
              </div>
              <div className="form-group">
                <input id="new-user-password" type="password" name="password" placeholder="Password"
                  value={userForm.password} onChange={handleUserFormChange} />
              </div>
              <div className="form-group">
                <select id="new-user-role" name="role" value={userForm.role} onChange={handleUserFormChange}
                  style={{ width:'100%', padding:'0.5rem 0.75rem', borderRadius:'6px',
                    border:'1px solid var(--border,#d1d5db)', fontSize:'0.95rem',
                    background:'var(--input-bg,#fff)' }}>
                  <option value="ROLE_PUBLISHER">Publisher</option>
                  <option value="ROLE_USER">User</option>
                  <option value="ROLE_ADMIN">Admin</option>
                </select>
              </div>
            </div>
            <button type="submit" className="btn" style={{ marginTop:'0.5rem' }}>Create Account</button>
          </form>

          {/* ── Search + Role filter ── */}
          {allUsers.length > 0 && (
            <>
              <div style={{ display:'flex', gap:'0.75rem', flexWrap:'wrap', marginBottom:'0.75rem', alignItems:'center' }}>
                {/* Search */}
                <div className="search-wrapper" style={{ flex:'1', minWidth:'180px' }}>
                  <span className="search-icon">🔍</span>
                  <input
                    type="text"
                    className="search-input"
                    placeholder="Search by name or email…"
                    value={userSearch}
                    onChange={(e) => setUserSearch(e.target.value)}
                  />
                  {userSearch && (
                    <button className="search-clear" onClick={() => setUserSearch('')} aria-label="Clear">✕</button>
                  )}
                </div>
                {/* Role filter pills */}
                {['ALL','ROLE_ADMIN','ROLE_PUBLISHER','ROLE_USER'].map((r) => (
                  <button
                    key={r}
                    onClick={() => setUserRoleFilter(r)}
                    style={{
                      padding: '0.3rem 0.9rem',
                      borderRadius: '999px',
                      border: '1.5px solid',
                      borderColor: userRoleFilter === r ? '#1e3a5f' : '#d1d5db',
                      background: userRoleFilter === r ? '#1e3a5f' : 'transparent',
                      color: userRoleFilter === r ? '#fff' : '#374151',
                      fontWeight: 600,
                      fontSize: '0.8rem',
                      cursor: 'pointer',
                      transition: 'all .15s',
                    }}
                  >
                    {{ ALL:'All', ROLE_ADMIN:'Admin', ROLE_PUBLISHER:'Publisher', ROLE_USER:'User' }[r]}
                  </button>
                ))}
              </div>

              {/* Scrollable table — max 5 rows visible */}
              <div style={{ overflowX:'auto' }}>
                <div style={{ maxHeight: '272px', overflowY: 'auto',
                    border: '1px solid var(--border,#e5e7eb)', borderRadius:'8px' }}>
                  <table className="books-table" style={{ marginBottom:0 }}>
                    <thead style={{ position:'sticky', top:0, zIndex:1,
                        background:'#1e3a5f', color:'#fff' }}>
                      <tr>
                        <th>Name</th>
                        <th>Email</th>
                        <th>Role</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredUsers.length === 0 ? (
                        <tr><td colSpan={4} style={{ textAlign:'center', color:'#9ca3af', padding:'1rem' }}>No users match your filter.</td></tr>
                      ) : filteredUsers.map((u) => (
                        <tr key={u.id}>
                          <td>{u.username || u.name}</td>
                          <td>{u.email}</td>
                          <td>
                            <span className={`role-badge role-badge--${
                              { ROLE_ADMIN:'admin', ROLE_PUBLISHER:'publisher', ROLE_USER:'user' }[u.role] || 'user'
                            }`}>
                              {{ ROLE_ADMIN:'Admin', ROLE_PUBLISHER:'Publisher', ROLE_USER:'User' }[u.role] || u.role}
                            </span>
                          </td>
                          <td>
                            {u.email !== currentUser?.email && (
                              <button className="btn btn--sm btn--danger" onClick={() => handleDeleteUser(u.id)}>Delete</button>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                <p style={{ fontSize:'0.78rem', color:'#9ca3af', marginTop:'0.4rem' }}>
                  Showing {filteredUsers.length} of {allUsers.length} users
                </p>
              </div>
            </>
          )}
        </div>
      )}

      {/* ── Books List ── */}
      <div className="card">
        <div className="books-header">
          <h2 className="section-title" style={{ marginBottom: 0 }}>Books</h2>
          <div className="books-controls">
            {/* Search */}
            <div className="search-wrapper">
              <span className="search-icon">🔍</span>
              <input
                id="book-search"
                type="text"
                className="search-input"
                placeholder="Search by title, author or genre…"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
              {searchQuery && (
                <button
                  className="search-clear"
                  onClick={() => setSearchQuery('')}
                  aria-label="Clear search"
                >✕</button>
              )}
            </div>
            {/* Sort */}
            <select
              id="book-sort"
              className="sort-select"
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value)}
            >
              <option value="default">Default order</option>
              <option value="rating-desc">⭐ Best Rated</option>
              <option value="rating-asc">↓ Lowest Rated</option>
              <option value="reviews">💬 Most Reviewed</option>
            </select>
          </div>
        </div>

        {q && (
          <p className="search-results-hint">
            {sorted.length === 0
              ? `No books found for "${searchQuery}"`
              : `${sorted.length} book${sorted.length !== 1 ? 's' : ''} found for "${searchQuery}"`}
          </p>
        )}

        <BookList
          books={sorted}
          currentUser={currentUser}
          onEditBook={showAddBook ? handleStartEditBook : null}
          onDeleteBook={showAddBook ? handleDeleteBook : null}
        />
      </div>

      {/* ── Add / Edit Review  (ROLE_USER only) ── */}
      {canPostReview(userRole) && (
        <div className="card">
          <h2 className="section-title">
            {editingReviewId
              ? 'Edit Your Review'
              : existingUserReview
              ? 'Update Your Review'
              : 'Add a Review'}
          </h2>
          {reviewError   && <div className="error-message"   role="alert">{reviewError}</div>}
          {reviewSuccess && <div className="success-message" role="status">{reviewSuccess}</div>}
          {existingUserReview && !editingReviewId && (
            <div className="existing-review-banner">
              <p className="review-update-note" style={{ marginBottom: '8px' }}>
                You've already reviewed this book — submitting will update your existing review.
              </p>
              <button
                type="button"
                className="btn btn--sm btn--danger"
                onClick={() => {
                  handleDeleteReview(existingUserReview.id);
                  setSelectedBookId('');
                  setReviewForm(emptyReviewForm);
                }}
              >
                Delete My Review
              </button>
            </div>
          )}
          <ReviewForm
            books={books}
            selectedBookId={selectedBookId}
            reviewForm={reviewForm}
            onBookSelect={setSelectedBookId}
            onChange={handleReviewChange}
            onSubmit={editingReviewId ? handleUpdateReview : handleAddReview}
            submitLabel={
              editingReviewId
                ? 'Update Review'
                : existingUserReview
                ? 'Update My Review'
                : 'Add Review'
            }
          />
          {editingReviewId && (
            <button
              type="button"
              className="btn btn--secondary"
              onClick={handleCancelEditReview}
              style={{ marginTop: '0.5rem' }}
            >
              Cancel
            </button>
          )}
        </div>
      )}

      {/* ── Reviews ── */}
      {visibleReviews.length > 0 && (
        <div className="card">
          <h2 className="section-title">
            {currentUser?.role === 'ROLE_PUBLISHER'
              ? 'Reviews for Your Books'
              : 'Reviews'}
          </h2>
          <div className="reviews-list">
            {visibleReviews.map((review) => (
              <div key={review.id} className="review-item">
                <div className="review-header">
                  <span className="review-book-title">{review.bookTitle}</span>
                  <span className="review-rating">
                    {'⭐'.repeat(review.rating)} {review.rating}/5
                  </span>
                </div>
                <p className="review-comment">{review.comment}</p>
                <div className="review-footer">
                  <span className="review-author">— {review.username || review.userId}</span>
                  {/* Edit — only the review owner (ROLE_USER) */}
                  {/* Delete — review owner OR admin (moderation) */}
                  {(canEditReview(currentUser, review) || canDeleteReview(currentUser, review)) && (
                    <div className="review-actions">
                      {canEditReview(currentUser, review) && (
                        <button
                          className="btn btn--sm"
                          onClick={() => handleStartEditReview(review)}
                        >
                          Edit
                        </button>
                      )}
                      {canDeleteReview(currentUser, review) && (
                        <button
                          className="btn btn--sm btn--danger"
                          onClick={() => handleDeleteReview(review.id)}
                        >
                          Delete
                        </button>
                      )}
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ── Recommendations — ROLE_USER only, never shown to Publisher or Admin ── */}
      {userRole === 'ROLE_USER' && recommendations.length > 0 && (
        <div className="card">
          <h2 className="section-title">Recommended for You</h2>
          <p className="recommendation-hint">
            Books you haven't reviewed yet, sorted by community rating.
          </p>
          <div className="recommendations-list">
            {recommendations.map((book) => (
              <div key={book.id} className="recommendation-item">
                <div className="recommendation-info">
                  <strong>{book.title}</strong>
                  <span className="recommendation-author">by {book.author}</span>
                  <span className="recommendation-genre">{book.genre}</span>
                </div>
                <span className="recommendation-rating">⭐ {book.averageRating}</span>
              </div>
            ))}
          </div>
        </div>
      )}

    </div>
  );
}

// ─── Root export ──────────────────────────────────────────────────────────────
export default function Dashboard({ onLogout, userRole, username, currentUser }) {
  const [store] = useState(createAppStore);
  return (
    <Provider store={store}>
      <AppContent
        onLogout={onLogout}
        userRole={userRole}
        username={username}
        currentUser={currentUser}
      />
    </Provider>
  );
}