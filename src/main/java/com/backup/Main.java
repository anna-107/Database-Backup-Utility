package com.backup;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Scanner;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println(" =======================DATABASE BACKUP UTILITY========================");
        System.out.println("Welcome, operator.\n");
        System.out.println("Press 1 to start");
        System.out.println("Press 0 or type --help for assistance");

        String input = scanner.nextLine().trim();

        if (input.equals("0") || input.equalsIgnoreCase("--help")) {
            showHelp();
            return;
        } else if (!input.equals("1")) {
            System.out.println(" Invalid option. Exiting.");
            return;
        }

        System.out.println("\n Starting interactive session...\n");

System.out.print("Enter Database Type (mysql/mongodb): ");
String dbType = scanner.nextLine().trim().toLowerCase();

// Declare variables outside to ensure scope
String host = null, user = null, password = null, database = null;
int port = 0;
String url = null, dataBaseName = null;

if (dbType.equals("mysql")) {
    System.out.print("Enter Host (e.g., localhost): ");
    host = scanner.nextLine().trim();

    System.out.print("Enter Port: ");
    port = Integer.parseInt(scanner.nextLine().trim());

    System.out.print("Enter Username: ");
    user = scanner.nextLine().trim();

    System.out.print("Enter Password: ");
    password = scanner.nextLine().trim();

    System.out.print("Enter Database Name: ");
    database = scanner.nextLine().trim();
} else if (dbType.equals("mongodb")) {
    System.out.println("Enter your mondb uri:");
    url = scanner.nextLine().trim();
    System.out.print("Enter Database Name: ");
    dataBaseName = scanner.nextLine().trim();
}

System.out.println("\n Testing connection...\n");

boolean isConnected = false;

if (dbType.equals("mysql")) {
    isConnected = testMySQLConnection(host, port, user, password, database);
} else if (dbType.equals("mongodb")) {
    isConnected = testMongoConnection(url, dataBaseName);
} else {
    System.out.println(" Unsupported DB type. Only mysql or mongodb are allowed.");
    return;
}

        if (isConnected) {
            System.out.println("\nConnection successful. Ready for backup operations.");
        
        } else {
            System.out.println("\nConnection failed. Check credentials or network access.");
        }
    }

    private static boolean testMySQLConnection(String host, int port, String user, String password, String db) {
        String url = "jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=false&serverTimezone=UTC";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("MySQL connection established.");
            return true;
        } catch (SQLException e) {
            System.out.println("MySQL Error: " + e.getMessage());
            return false;
        }
    }

    private static boolean testMongoConnection(String uri, String db) {
        try (MongoClient mongoClient = MongoClients.create(uri)) {
            MongoDatabase mongoDatabase = mongoClient.getDatabase(db);
            mongoDatabase.listCollectionNames().first(); // basic check
            System.out.println("MongoDB connection established.");
            return true;
        } catch (Exception e) {
            System.out.println("MongoDB Error: " + e.getMessage());
            return false;
        }
    }

    private static void showHelp() {
        System.out.println("\n=== HELP MENU ===");
        System.out.println("This tool allows you to backup MySQL or MongoDB databases.");
        System.out.println("1 - Start backup setup");
        System.out.println("0 or --help - Show this help menu");
    }
}
