# Book Review Application - Setup Instructions

## Quick Start

### Option 1: Use the startup script (Recommended)
```bash
./start.sh
```

### Option 2: Manual startup

#### Start Backend (Spring Boot)
```bash
cd springapp
./mvnw spring-boot:run
```

#### Start Frontend (React) - In a new terminal
```bash
cd reactapp
npm install
npm start
```

## Application URLs
- Frontend: http://localhost:8081
- Backend API: http://localhost:8080/api
- H2 Database Console: http://localhost:8080/h2-console

## Database Configuration
The application uses H2 in-memory database with the following settings:
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (empty)

## API Endpoints
- GET `/api/books` - Get all books with average ratings
- POST `/api/books` - Add a new book
- GET `/api/reviews?bookId={id}` - Get reviews for a book
- POST `/api/reviews` - Add a new review

## Running Tests

### Backend Tests
```bash
cd springapp
./mvnw test
```

### Frontend Tests
```bash
cd reactapp
npm test
```

## Features
- Add new books with title, author, genre, and description
- View all books in a table format
- Add reviews with ratings (1-5) and comments
- Automatic calculation of average ratings
- Responsive UI with proper form validation