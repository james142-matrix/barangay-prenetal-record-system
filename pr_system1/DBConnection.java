/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package pr_system1;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *
 * @author User PC
 */
public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/pr_system"; // your database name
    private static final String USER = "root"; // your MySQL username
    private static final String PASSWORD = ""; // leave blank if none

    public static Connection getConnection() {
        try {
            // ✅ Correct MySQL driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException("Database connection error: " + e.getMessage());
        }
    }
}
