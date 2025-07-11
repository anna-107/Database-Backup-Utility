package com.backup.service;

import java.util.Scanner;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQLService implements DatabaseService {

    private final String host;
    private final int port;
    private final String user;
    private final String password;
    private final String database;

    public MySQLService() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter Host: ");
        this.host = scanner.nextLine().trim();
        System.out.print("Enter Port: ");
        this.port = Integer.parseInt(scanner.nextLine().trim());;
        System.out.print("Enter Username: ");
        this.user = scanner.nextLine().trim();
        System.out.print("Enter Password: ");
        this.password = scanner.nextLine().trim();
        System.out.print("Enter Database Name: ");
        this.database = scanner.nextLine().trim();
    }


    @Override
    public boolean testConnection() {
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database +
                     "?useSSL=false&serverTimezone=UTC";
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println(" MySQL connection established.");
            return true;
        } catch (SQLException e) {
            System.out.println(" MySQL connection failed: " + e.getMessage());
            return false;
        }
    }
}
