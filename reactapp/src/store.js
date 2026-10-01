import { configureStore } from '@reduxjs/toolkit';
import booksReducer from './features/books/booksSlice';

export const createAppStore = () => configureStore({
  reducer: {
    books: booksReducer
  }
});

export default createAppStore;
