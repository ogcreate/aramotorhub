package com.ogcreate.app.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/aramotorhubdb"; // Replace with your DB name
    private static final String USER = "root"; // Default MySQL user
    private static final String PASSWORD = ""; // Default is blank in XAMPP

    public static Connection connect() {
        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connected to database");
            return conn;
        } catch (SQLException e) {
            System.out.print("Connection failed: " + e.getMessage());
            return null;
        }
    }
}

