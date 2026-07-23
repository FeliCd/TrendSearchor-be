package com.fpt.swp;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DbRepairTest {

    @Test
    @Disabled("Manual utility test for repairing local MySQL Flyway table")
    public void testRepairFlyway() throws Exception {
        String url = "jdbc:mysql://localhost:3306/trendsearchor";
        String user = "root";
        String password = "root123";
        
        System.out.println("Connecting to database to repair Flyway history...");
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            int rows = stmt.executeUpdate("DELETE FROM flyway_schema_history WHERE success = 0");
            System.out.println("Successfully deleted " + rows + " failed Flyway migration rows.");
        } catch (Exception e) {
            System.err.println("Error repairing database: " + e.getMessage());
            throw e;
        }
    }
}
