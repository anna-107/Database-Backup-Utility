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
    
    @Override
    public boolean fullBackup() {
        String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        String backupDir = "backups";
        new java.io.File(backupDir).mkdirs(); // create backups dir if it does not exist

        String fileName = database + "_backup_" + timestamp + ".sql";
        String filePath = backupDir + java.io.File.separator + fileName;

        String cmd = String.format(
                "mysqldump -h%s -P%d -u%s -p%s %s -r %s",
                host, port, user, password, database, filePath
        );

        System.out.println(" Running backup: " + fileName);

        try {
            Process proc = Runtime.getRuntime().exec(cmd);
            int exitCode = proc.waitFor();

            if (exitCode == 0) {
                System.out.println("Congrats!  Backup completed. File is at→ " + filePath);
                return true;
            } else {
                System.out.println("Backup failed. Exit code: " + exitCode);
                return false;
            }
        } catch (IOException | InterruptedException e) {
            System.out.println(" Exception during backup: " + e.getMessage());
            return false;
        }
    }

}
