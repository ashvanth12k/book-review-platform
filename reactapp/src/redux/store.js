import { configureStore } from '@reduxjs/toolkit';
import booksReducer from './booksSlice';

export const createAppStore = () =>
  configureStore({
    reducer: {
      books: booksReducer,
    },
  });

export default createAppStore;
