#!/bin/bash

echo "Starting Book Review Application..."

# Start backend
echo "Starting Spring Boot backend..."
cd springapp
mvn spring-boot:run &
BACKEND_PID=$!

# Wait for backend to start
echo "Waiting for backend to start..."
sleep 30

# Start frontend
echo "Starting React frontend..."
cd ../reactapp
npm start &
FRONTEND_PID=$!

echo "Application started!"
echo "Backend running on: http://localhost:8080"
echo "Frontend running on: http://localhost:8081"
echo "Press Ctrl+C to stop both services"

# Wait for user to stop
wait $BACKEND_PID $FRONTEND_PID