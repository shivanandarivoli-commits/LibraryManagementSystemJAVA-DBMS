
# 📚 Library Management System

A simple web-based Library Management System built using Java and MySQL.

## ✨ Features

- Add new books
- View all books
- Search for books
- Delete books
- Store book records in a MySQL database
- Simple web interface

## 🛠️ Technologies Used

- Java
- Java HTTP Server
- JDBC (Java Database Connectivity)
- MySQL 8.4
- HTML

## 🗄️ Database

Database name: `library_db`

Table name: `books`

| Column | Type |
|---|---|
| id | INT (Primary Key) |
| title | VARCHAR(150) |
| author | VARCHAR(100) |

## ▶️ How to Run

1. Start the MySQL database.
2. Compile the Java program:

   `javac -cp lib/mysql-connector-j.jar -d out src/main/java/com/library/Main.java`

3. Start the application:

   `java -cp "out:lib/mysql-connector-j.jar" com.library.Main`

4. Open the forwarded port `8082` in your browser.

## 👨‍💻 Project Purpose

This project demonstrates Java programming, JDBC connectivity, MySQL database operations, and basic web application development.

## 📌 Operations

The application supports basic CRUD-related operations:
- Create: Add books
- Read: View and search books
- Delete: Remove books
