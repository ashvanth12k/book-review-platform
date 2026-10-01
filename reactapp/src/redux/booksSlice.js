import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import * as api from '../api';


export const fetchBooks = createAsyncThunk('books/fetchBooks', async () => {
  const data = await api.getBooks();
  return Array.isArray(data) ? data : [];
});

export const createBook = createAsyncThunk(
  'books/createBook',
  async ({ book, currentUser }, { dispatch }) => {
    // book may include publisherEmail (admin flow only)
    await api.addBook(book, currentUser);
    return dispatch(fetchBooks()).unwrap();
  }
);

export const editBook = createAsyncThunk(
  'books/editBook',
  async ({ id, updates, currentUser }, { dispatch }) => {
    await api.updateBook(id, updates, currentUser);
    return dispatch(fetchBooks()).unwrap();
  }
);

export const removeBook = createAsyncThunk(
  'books/removeBook',
  async ({ id, currentUser }, { dispatch }) => {
    await api.deleteBook(id, currentUser);
    return dispatch(fetchBooks()).unwrap();
  }
);


export const createReview = createAsyncThunk(
  'books/createReview',
  async ({ bookId, review, currentUser }, { dispatch }) => {
    await api.addReview(bookId, review, currentUser);
    return dispatch(fetchBooks()).unwrap();
  }
);

export const editReview = createAsyncThunk(
  'books/editReview',
  async ({ reviewId, updates, currentUser }, { dispatch }) => {
    await api.updateReview(reviewId, updates, currentUser);
    return dispatch(fetchBooks()).unwrap();
  }
);

export const removeReview = createAsyncThunk(
  'books/removeReview',
  async ({ reviewId, currentUser }, { dispatch }) => {
    await api.deleteReview(reviewId, currentUser);
    return dispatch(fetchBooks()).unwrap();
  }
);

const booksSlice = createSlice({
  name: 'books',
  initialState: {
    items:  [],
    status: 'idle',   
    error:  null,
  },
  reducers: {},
  extraReducers: (builder) => {
    
    builder
      .addCase(fetchBooks.pending,   (state) => { state.status = 'loading'; state.error = null; })
      .addCase(fetchBooks.fulfilled, (state, action) => { state.items = action.payload; state.status = 'succeeded'; })
      .addCase(fetchBooks.rejected,  (state, action) => { state.items = []; state.status = 'failed'; state.error = action.error.message || 'Unable to load books'; });

    const mutating = [createBook, editBook, removeBook, createReview, editReview, removeReview];
    mutating.forEach((thunk) => {
      builder
        .addCase(thunk.pending,   (state) => { state.error = null; })
        .addCase(thunk.fulfilled, (state, action) => { state.items = action.payload; state.status = 'succeeded'; state.error = null; })
        .addCase(thunk.rejected,  (state, action) => { state.error = action.error.message || 'Operation failed'; });
    });
  },
});

export default booksSlice.reducer;
