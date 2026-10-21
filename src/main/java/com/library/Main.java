
package com.library;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

public class Main {

    static final String URL = "jdbc:mysql://127.0.0.1:3306/library_db";
    static final String USER = "root";
    static final String PASSWORD = "LibraryPass123!";

    static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    static String page(String message) {
        StringBuilder rows = new StringBuilder();

        try (Connection con = connect();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT id,title,author FROM books ORDER BY id")) {

            while (rs.next()) {
                rows.append("<tr><td>").append(rs.getInt("id"))
                    .append("</td><td>").append(esc(rs.getString("title")))
                    .append("</td><td>").append(esc(rs.getString("author")))
                    .append("</td></tr>");
            }

            if (rows.length() == 0) {
                rows.append("<tr><td colspan='3'>No books added yet</td></tr>");
            }
        } catch (SQLException e) {
            rows.append("<tr><td colspan='3'>Database error: ")
                .append(esc(e.getMessage())).append("</td></tr>");
        }

        return """
            <!DOCTYPE html><html><head>
            <title>Library Management System</title>
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <style>
            body{font-family:Arial;background:#f2f5fa;padding:20px}
            .card{background:white;max-width:750px;margin:20px auto;padding:24px;
            border-radius:12px;box-shadow:0 3px 12px #ccd}
            h1,h2{text-align:center;color:#17365d}
            input{box-sizing:border-box;width:100%;padding:12px;margin:8px 0;
            border:1px solid #ccc;border-radius:6px}
            button{width:100%;padding:12px;margin:8px 0;background:#1769aa;
            color:white;border:0;border-radius:6px;cursor:pointer}
            .delete{background:#c0392b}
            table{width:100%;border-collapse:collapse;margin-top:15px}
            th,td{padding:10px;border:1px solid #ddd;text-align:left}
            th{background:#1769aa;color:white}
            .message{text-align:center;color:green;font-weight:bold}
            </style></head><body><div class="card">
            <h1>Library Management System</h1>
            <p style="text-align:center">Java Web Application + MySQL</p>

            <h2>Add Book</h2>
            <form method="POST" action="/add">
            <input type="number" name="id" placeholder="Book ID" min="1" required>
            <input name="title" placeholder="Book Title" required>
            <input name="author" placeholder="Author Name" required>
            <button type="submit">Save Book</button></form>

            <h2>Search Book</h2>
            <form method="GET" action="/search">
            <input type="number" name="id" placeholder="Book ID to search" required>
            <button type="submit">Search Book</button></form>

            <h2>Delete Book</h2>
            <form method="POST" action="/delete"
            onsubmit="return confirm('Delete this book?')">
            <input type="number" name="id" placeholder="Book ID to delete" required>
            <button class="delete" type="submit">Delete Book</button></form>

            <p class="message">""" + esc(message) + """
            </p><h2>All Books</h2>
            <table><tr><th>ID</th><th>Title</th><th>Author</th></tr>
            """ + rows + """
            </table></div></body></html>
            """;
    }

    static void send(HttpExchange ex, String html) throws Exception {
        byte[] data = html.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        ex.sendResponseHeaders(200, data.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(data);
        }
    }

    static Map<String,String> formData(String data) {
        Map<String,String> map = new HashMap<>();
        if (data == null || data.isEmpty()) return map;

        for (String part : data.split("&")) {
            String[] pair = part.split("=", 2);
            String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
            String value = pair.length > 1
                ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "";
            map.put(key, value);
        }
        return map;
    }

    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");

        try (Connection con = connect();
             Statement st = con.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS books (
                    id INT PRIMARY KEY,
                    title VARCHAR(150) NOT NULL,
                    author VARCHAR(100) NOT NULL
                )
                """);
        }

        HttpServer server = HttpServer.create(
            new InetSocketAddress("0.0.0.0", 8082), 0);

        server.createContext("/", ex -> {
            try {
                send(ex, page(""));
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                ex.close();
            }
        });

        server.createContext("/add", ex -> {
            try {
                Map<String,String> d = formData(new String(
                    ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                String id = d.getOrDefault("id", "").trim();
                String title = d.getOrDefault("title", "").trim();
                String author = d.getOrDefault("author", "").trim();
                String msg;

                if (id.isEmpty() || title.isEmpty() || author.isEmpty()) {
                    msg = "Please fill in every field.";
                } else {
                    try (Connection con = connect();
                         PreparedStatement ps = con.prepareStatement(
                             "INSERT INTO books(id,title,author) VALUES(?,?,?)")) {
                        ps.setInt(1, Integer.parseInt(id));
                        ps.setString(2, title);
                        ps.setString(3, author);
                        ps.executeUpdate();
                        msg = "Book added to MySQL successfully!";
                    } catch (SQLIntegrityConstraintViolationException e) {
                        msg = "That Book ID already exists.";
                    } catch (NumberFormatException e) {
                        msg = "Book ID must be a number.";
                    } catch (SQLException e) {
                        msg = "Database error: " + e.getMessage();
                    }
                }
                send(ex, page(msg));
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                ex.close();
            }
        });

        server.createContext("/search", ex -> {
            try {
                Map<String,String> d = formData(ex.getRequestURI().getRawQuery());
                String id = d.getOrDefault("id", "");
                String msg = "Book not found.";

                try (Connection con = connect();
                     PreparedStatement ps = con.prepareStatement(
                         "SELECT title,author FROM books WHERE id=?")) {
                    ps.setInt(1, Integer.parseInt(id));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            msg = "Found: " + rs.getString("title")
                                + " by " + rs.getString("author")
                                + " (ID: " + id + ")";
                        }
                    }
                } catch (SQLException | NumberFormatException e) {
                    msg = "Search failed: " + e.getMessage();
                }
                send(ex, page(msg));
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                ex.close();
            }
        });

        server.createContext("/delete", ex -> {
            try {
                Map<String,String> d = formData(new String(
                    ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                String id = d.getOrDefault("id", "");
                String msg;

                try (Connection con = connect();
                     PreparedStatement ps = con.prepareStatement(
                         "DELETE FROM books WHERE id=?")) {
                    ps.setInt(1, Integer.parseInt(id));
                    msg = ps.executeUpdate() > 0
                        ? "Book deleted from MySQL successfully!"
                        : "Book ID not found.";
                } catch (SQLException | NumberFormatException e) {
                    msg = "Delete failed: " + e.getMessage();
                }
                send(ex, page(msg));
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                ex.close();
            }
        });

        server.start();
        System.out.println("Library app running on port 8082");
    }
}
