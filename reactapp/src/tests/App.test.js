import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import Dashboard from '../Dashboard';
import * as apiService from '../api';

jest.mock('../api');

const mockBooks = [
  {
    id: 1,
    title: 'Book A',
    author: 'Author A',
    genre: 'Fiction',
    description: 'Desc A',
    averageRating: 4,
    reviews: [],
    publisherId: 10,
  },
  {
    id: 2,
    title: 'Book B',
    author: 'Author B',
    genre: 'Non-fiction',
    description: 'Desc B',
    averageRating: 5,
    reviews: [],
    publisherId: 10,
  },
];

const mockNewBook = {
  id: 3,
  title: 'Book C',
  author: 'Author C',
  genre: 'Sci-fi',
  description: 'Desc C',
  averageRating: null,
  reviews: [],
  publisherId: 10,
};

const publisherUser = {
  id: 10,
  username: 'sunpublisher',
  email: 'sunpublisher@gmail.com',
  role: 'ROLE_PUBLISHER',
};


const regularUser = {
  id: 20,
  username: 'testuser',
  email: 'user@test.com',
  role: 'ROLE_USER',
};

describe('Book Review & Rating Platform - Tests', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    apiService.getBooks.mockResolvedValue(mockBooks);
    apiService.addBook.mockResolvedValue(mockNewBook);
    apiService.addReview.mockResolvedValue({});
    apiService.getAdminStats.mockResolvedValue(null);
    apiService.getRecommendations.mockResolvedValue([]);
  });


  function renderAsPublisher() {
    return render(
      <Dashboard
        onLogout={() => {}}
        userRole={publisherUser.role}
        username={publisherUser.username}
        currentUser={publisherUser}
      />
    );
  }

  // Helper: render Dashboard as a regular USER (can see Add Review form)
  function renderAsUser() {
    return render(
      <Dashboard
        onLogout={() => {}}
        userRole={regularUser.role}
        username={regularUser.username}
        currentUser={regularUser}
      />
    );
  }

  // 2 ✅ Creates new book and refreshes list (publisher)
  test('React_APIIntegration_TestingAndAPIDocumentation_creates new book and updates list', async () => {
    renderAsPublisher();

    // Wait for books to load
    await waitFor(() => expect(apiService.getBooks).toHaveBeenCalled());

    // The search bar placeholder "Search by title, author or genre…" also matches
    // /Title/i, /Author/i and /Genre/i — use getAllByPlaceholderText()[0] for form fields
    fireEvent.change(screen.getAllByPlaceholderText(/Title/i)[0],  { target: { value: 'Book C' } });
    fireEvent.change(screen.getAllByPlaceholderText(/Author/i)[0], { target: { value: 'Author C' } });
    fireEvent.change(screen.getAllByPlaceholderText(/Genre/i)[0],  { target: { value: 'Sci-fi' } });
    fireEvent.change(screen.getByPlaceholderText(/Description/i), { target: { value: 'Desc C' } });
    fireEvent.click(screen.getByRole('button', { name: /Add Book/i }));

    await waitFor(() =>
      expect(apiService.addBook).toHaveBeenCalledWith(
        { title: 'Book C', author: 'Author C', genre: 'Sci-fi', description: 'Desc C' },
        publisherUser
      )
    );
    await waitFor(() => expect(apiService.getBooks).toHaveBeenCalledTimes(2));
  });

  // 3 ✅ Renders book table headers
  test('React_BuildUIComponents_renders book table headers', async () => {
    renderAsPublisher();
    expect(await screen.findByText(/Title/i)).toBeInTheDocument();
    expect(screen.getByText(/Author/i)).toBeInTheDocument();
    expect(screen.getByText(/Genre/i)).toBeInTheDocument();
    expect(screen.getByText(/Description/i)).toBeInTheDocument();
    expect(screen.getByText(/Average Rating/i)).toBeInTheDocument();
  });

  // 4 ✅ Book form input updates state
  test('React_BuildUIComponents_book form input updates state', async () => {
    renderAsPublisher();
    await waitFor(() => expect(apiService.getBooks).toHaveBeenCalled());
    // [0] = BookForm title input; search bar placeholder also contains "title"
    const input = screen.getAllByPlaceholderText(/Title/i)[0];
    fireEvent.change(input, { target: { value: 'My Book' } });
    expect(input.value).toBe('My Book');
  });

  // 5 ✅ Does not call addBook if form empty
  test('React_BuildUIComponents_does not call addBook if form is empty', async () => {
    renderAsPublisher();
    await waitFor(() => expect(apiService.getBooks).toHaveBeenCalled());
    fireEvent.click(screen.getByRole('button', { name: /Add Book/i }));
    expect(apiService.addBook).not.toHaveBeenCalled();
  });

  // 6 ✅ Clears form after successful add
  test('React_BuildUIComponents_clears form after adding book', async () => {
    renderAsPublisher();
    await waitFor(() => expect(apiService.getBooks).toHaveBeenCalled());
    const titleInput = screen.getAllByPlaceholderText(/Title/i)[0];
    fireEvent.change(titleInput,                                      { target: { value: 'Book C' } });
    fireEvent.change(screen.getAllByPlaceholderText(/Author/i)[0],    { target: { value: 'Author C' } });
    fireEvent.change(screen.getAllByPlaceholderText(/Genre/i)[0],     { target: { value: 'Sci-fi' } });
    fireEvent.change(screen.getByPlaceholderText(/Description/i),    { target: { value: 'Desc C' } });
    fireEvent.click(screen.getByRole('button', { name: /Add Book/i }));
    await waitFor(() => expect(titleInput.value).toBe(''));
  });

  // 7 ✅ Renders "No reviews yet" if no average rating
  test('React_UITestingAndResponsivenessFixes_renders No reviews yet when no averageRating', async () => {
    apiService.getBooks.mockResolvedValue([
      { id: 1, title: 'Book X', author: 'Author X', genre: 'Drama', description: 'Desc X', reviews: [], publisherId: null },
    ]);
    renderAsPublisher();
    expect(await screen.findByText(/No reviews yet/i)).toBeInTheDocument();
  });

  // 10 ✅ Does not call addReview if form is empty (regular user)
  test('React_BuildUIComponents_does not call addReview if form empty', async () => {
    renderAsUser();
    await waitFor(() => expect(apiService.getBooks).toHaveBeenCalled());
    fireEvent.click(await screen.findByRole('button', { name: /Add Review/i }));
    expect(apiService.addReview).not.toHaveBeenCalled();
  });

  // 12 ✅ Works when book list is empty
  test('React_BuildUIComponents_renders empty book list correctly', async () => {
    apiService.getBooks.mockResolvedValue([]);
    renderAsPublisher();
    await waitFor(() => {
      expect(screen.queryByText('Book A')).not.toBeInTheDocument();
    });
  });

  // 14 ✅ Handles API error gracefully
  test('React_UITestingAndResponsivenessFixes_handles API error gracefully', async () => {
    apiService.getBooks.mockRejectedValue(new Error('API Error'));
    renderAsPublisher();
    await waitFor(() => {
      // No crash — app header is always visible
      expect(screen.getByText(/Book Review Platform/i)).toBeInTheDocument();
    });
  });

  // 15 ✅ Allows entering numeric rating (regular user sees rating input)
  test('React_BuildUIComponents_rating input accepts numbers', async () => {
    renderAsUser();
    await waitFor(() => expect(apiService.getBooks).toHaveBeenCalled());
    const ratingInput = await screen.findByPlaceholderText(/Rating/i);
    fireEvent.change(ratingInput, { target: { value: '3' } });
    expect(ratingInput.value).toBe('3');
  });
});
