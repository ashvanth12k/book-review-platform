import axios from 'axios';

const BASE_URL = 'http://localhost:8082';

const apiClient = axios.create({ baseURL: BASE_URL });

function getStoredAuth() {
  try {
    const raw = localStorage.getItem('authUser');
    return raw ? JSON.parse(raw) : null;
  } catch { return null; }
}
function getAccessToken()  { return getStoredAuth()?.accessToken  || null; }
function getRefreshToken() { return getStoredAuth()?.refreshToken || null; }

function updateStoredTokens(accessToken, refreshToken) {
  const current = getStoredAuth();
  if (current) {
    localStorage.setItem('authUser', JSON.stringify({ ...current, accessToken, refreshToken }));
  }
}

function clearAuthAndRedirect() {
  localStorage.removeItem('authUser');
  window.location.replace('/login');
}

// ── Request interceptor: attach Bearer token ──────────────────────────────────
apiClient.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token) config.headers['Authorization'] = `Bearer ${token}`;
  return config;
});

// ── Response interceptor: auto-refresh on 401, then retry ────────────────────
let isRefreshing = false;
let pendingQueue = [];   // requests waiting for the new token

function drainQueue(error, newToken) {
  pendingQueue.forEach(({ resolve, reject }) =>
    error ? reject(error) : resolve(newToken)
  );
  pendingQueue = [];
}

apiClient.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config;

    if (error.response?.status === 401 && !original._retry) {
      // Queue extra requests while a refresh is in-flight
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          pendingQueue.push({ resolve, reject });
        }).then((newToken) => {
          original.headers['Authorization'] = `Bearer ${newToken}`;
          return apiClient(original);
        });
      }

      original._retry = true;
      isRefreshing = true;

      const raw = getRefreshToken();
      if (!raw) { clearAuthAndRedirect(); return Promise.reject(error); }

      try {
        const res = await axios.post(`${BASE_URL}/api/users/refresh-token`, { refreshToken: raw });
        const { token: newAccess, refreshToken: newRefresh } = res.data;
        updateStoredTokens(newAccess, newRefresh);
        drainQueue(null, newAccess);
        original.headers['Authorization'] = `Bearer ${newAccess}`;
        return apiClient(original);
      } catch (refreshErr) {
        drainQueue(refreshErr, null);
        clearAuthAndRedirect();
        return Promise.reject(refreshErr);
      } finally {
        isRefreshing = false;
      }
    }

    // Extract the most meaningful error message from the response
    const msg =
      error.response?.data?.error   ||
      error.response?.data?.message ||
      error.message                 ||
      'Request failed';
    return Promise.reject(new Error(msg));
  }
);

// ── Field mapping helpers ─────────────────────────────────────────────────────

/** Backend ReviewResponse → frontend review shape. */
function mapReview(r) {
  return {
    id:      r.id,
    bookId:  r.bookId,
    userId:  r.userId,
    comment: r.reviewText || '',   // backend 'reviewText' → frontend 'comment'
    rating:  r.rating,
  };
}

/** Backend Book → frontend book shape. */
function mapBook(b, reviewsByBookId) {
  return {
    id:            b.id,
    title:         b.title,
    author:        b.author,
    genre:         b.genre,
    description:   b.description,
    publisherId:   b.publisherId ?? (b.publisher?.id ?? null),
    averageRating: b.averageRating ?? null,
    reviews:       reviewsByBookId[b.id] || [],
  };
}

// ─────────────────────────────────────────────────────────────────────────────
// AUTH
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Login: returns { accessToken, refreshToken, user: { id, username, email, role } }
 * Caller (LoginPage) passes this object to AuthContext.login().
 */
export async function loginUser(email, password) {
  const loginRes = await apiClient.post('/api/users/login', { email, password });
  const { token: accessToken, refreshToken, role } = loginRes.data;

  // Use plain axios (not apiClient) so the request interceptor does NOT overwrite
  // the Authorization header — the token isn't in localStorage yet at this point.
  const meRes = await axios.get(`${BASE_URL}/api/users/me`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  const me = meRes.data; // { id, name, email, role }

  return {
    accessToken,
    refreshToken,
    user: {
      id:       me.id,
      username: me.name,          // backend 'name' → frontend 'username'
      email:    me.email,
      role:     me.role || role,
    },
  };
}


/**
 * Register: creates a ROLE_USER account.
 * backend RegisterRequest expects { name, email, password }.
 */
export async function registerUser(username, email, password) {
  const res = await apiClient.post('/api/users/register', {
    name: username,   // backend field is 'name'
    email,
    password,
  });
  return res.data;
}

/**
 * Logout: revokes the refresh token on the backend.
 * Errors are swallowed — local auth is cleared regardless.
 */
export async function logoutUser() {
  try { await apiClient.post('/api/users/logout'); } catch { /* ignore */ }
}

// ─────────────────────────────────────────────────────────────────────────────
// BOOKS
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Fetch all books + all reviews, then assemble book.reviews[] so the
 * Redux slice and Dashboard.jsx require no structural changes.
 */
export async function getBooks() {
  const [booksRes, reviewsRes] = await Promise.all([
    apiClient.get('/api/books'),
    apiClient.get('/api/reviews'),
  ]);

  const allReviews = reviewsRes.data.map(mapReview);

  // Group reviews by bookId for O(1) lookup
  const reviewsByBookId = {};
  allReviews.forEach((r) => {
    if (!reviewsByBookId[r.bookId]) reviewsByBookId[r.bookId] = [];
    reviewsByBookId[r.bookId].push(r);
  });

  return booksRes.data.map((b) => mapBook(b, reviewsByBookId));
}

export async function getBookById(id) {
  const res = await apiClient.get(`/api/books/${id}`);
  return res.data;
}

export async function addBook(book, currentUser) {
  if (!currentUser ||
      (currentUser.role !== 'ROLE_PUBLISHER' && currentUser.role !== 'ROLE_ADMIN')) {
    throw new Error('Only publishers and admins can add books.');
  }
  const payload = {
    title: book.title,
    author: book.author,
    genre: book.genre,
    description: book.description,
  };
  // Admin can assign a book to a specific publisher by email
  if (currentUser.role === 'ROLE_ADMIN' && book.publisherEmail) {
    payload.publisherEmail = book.publisherEmail;
  }
  const res = await apiClient.post('/api/books', payload);
  return res.data;
}

export async function updateBook(id, updates, currentUser) {
  if (!currentUser ||
      (currentUser.role !== 'ROLE_PUBLISHER' && currentUser.role !== 'ROLE_ADMIN')) {
    throw new Error('Only publishers and admins can edit books.');
  }
  const res = await apiClient.put(`/api/books/${id}`, {
    title: updates.title, author: updates.author, genre: updates.genre, description: updates.description,
  });
  return res.data;
}

/**
 * Delete a book.
 * - ADMIN: uses DELETE /api/books/{id} (any book).
 * - PUBLISHER: uses DELETE /api/publisher/books/{id} (own books only; backend enforces ownership).
 */
export async function deleteBook(id, currentUser) {
  if (!currentUser) throw new Error('Not authenticated.');
  if (currentUser.role === 'ROLE_ADMIN') {
    await apiClient.delete(`/api/books/${id}`);
  } else if (currentUser.role === 'ROLE_PUBLISHER') {
    await apiClient.delete(`/api/publisher/books/${id}`);
  } else {
    throw new Error('Only publishers and admins can delete books.');
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// REVIEWS
// ─────────────────────────────────────────────────────────────────────────────

export async function addReview(bookId, review, currentUser) {
  if (!currentUser || currentUser.role !== 'ROLE_USER') {
    throw new Error('Only users can post reviews.');
  }
  const res = await apiClient.post('/api/reviews', {
    bookId:     Number(bookId),
    reviewText: review.comment,   // frontend 'comment' → backend 'reviewText'
    rating:     Number(review.rating),
  });
  return mapReview(res.data);
}

export async function updateReview(reviewId, updates, currentUser) {
  const body = {
    reviewText: updates.comment,
    rating:     Number(updates.rating),
  };
  // Only include bookId if it's actually provided (PUT doesn't require it,
  // the review already has one stored, but we send it anyway for completeness).
  if (updates.bookId != null && updates.bookId !== '') {
    body.bookId = Number(updates.bookId);
  }
  const res = await apiClient.put(`/api/reviews/${reviewId}`, body);
  return mapReview(res.data);
}

export async function deleteReview(reviewId, currentUser) {
  await apiClient.delete(`/api/reviews/${reviewId}`);
}

// ─────────────────────────────────────────────────────────────────────────────
// ADMIN
// ─────────────────────────────────────────────────────────────────────────────

/** Returns { totalUsers, totalBooks, totalReviews, averageRating } */
export async function getAdminStats() {
  const res = await apiClient.get('/api/admin/statistics');
  return res.data;
}

export async function getAllUsers() {
  const res = await apiClient.get('/api/admin/users');
  return res.data;
}

export async function deleteUser(id) {
  await apiClient.delete(`/api/admin/users/${id}`);
}

export async function promoteUser(id, role) {
  const res = await apiClient.post(`/api/users/${id}/promote`, { role });
  return res.data;
}

/**
 * Admin: create a new user or publisher account.
 * @param {Object} userData - { name, email, password, role }
 */
export async function createAdminUser(userData) {
  const res = await apiClient.post('/api/admin/users', userData);
  return res.data;
}

/**
 * Admin: returns list of all ROLE_PUBLISHER users.
 * Used to populate the publisher dropdown in the Add Book form.
 */
export async function getPublishers() {
  const res = await apiClient.get('/api/admin/publishers');
  return res.data;
}

// ─────────────────────────────────────────────────────────────────────────────
// RECOMMENDATIONS
// ─────────────────────────────────────────────────────────────────────────────

/** USER-only: returns personalized book list from backend. */
export async function getRecommendations() {
  const res = await apiClient.get('/api/recommendations');
  return res.data;
}

// ─────────────────────────────────────────────────────────────────────────────
// PERMISSION HELPERS — pure functions, no HTTP call
// Used by Dashboard.jsx and BookList.jsx for UI gating.
// The backend enforces identical rules server-side.
// ─────────────────────────────────────────────────────────────────────────────

/** PUBLISHER (own book) or ADMIN can edit books. */
export function canManageBook(currentUser, book) {
  if (!currentUser) return false;
  if (currentUser.role === 'ROLE_ADMIN') return true;
  if (currentUser.role === 'ROLE_PUBLISHER') {
    // Compare as numbers since backend IDs are Long
    return Number(book.publisherId) === Number(currentUser.id);
  }
  return false;
}

/**
 * ADMIN can delete any book.
 * PUBLISHER can delete their own books (backend enforces ownership via /api/publisher/books/{id}).
 */
export function canDeleteBook(currentUser, book) {
  if (!currentUser) return false;
  if (currentUser.role === 'ROLE_ADMIN') return true;
  if (currentUser.role === 'ROLE_PUBLISHER' && book) {
    return Number(book.publisherId) === Number(currentUser.id);
  }
  return false;
}

/** Only the review's author (ROLE_USER) can edit their own review. */
export function canEditReview(currentUser, review) {
  if (!currentUser || currentUser.role !== 'ROLE_USER') return false;
  return Number(review.userId) === Number(currentUser.id);
}

/** Review author (ROLE_USER) or ADMIN (moderation) can delete. */
export function canDeleteReview(currentUser, review) {
  if (!currentUser) return false;
  if (currentUser.role === 'ROLE_ADMIN') return true;
  if (currentUser.role === 'ROLE_USER') {
    return Number(review.userId) === Number(currentUser.id);
  }
  return false;
}