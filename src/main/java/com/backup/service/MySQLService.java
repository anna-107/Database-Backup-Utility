package com.backup.service;

import com.backup.FileUtils;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Scanner;

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
        String backupDir = FileUtils.createTimestampFolder("mysql", database);
        String filePath = backupDir + File.separator + database + ".sql";
        String cmd = String.format(
                "mysqldump -h%s -P%d -u%s -p%s %s -r %s",
                host, port, user, password, database, filePath
        );

        System.out.println(" Running backup: " + filePath);

        try {
            Process proc = Runtime.getRuntime().exec(cmd);
            int exitCode = proc.waitFor();

            if (exitCode == 0) {
                System.out.println("Backup completed → " + filePath);
                String zipPath = backupDir + ".zip";
                if (FileUtils.zipDirectory(backupDir, zipPath)) {
                    System.out.println("Backup zipped → " + zipPath);
                    FileUtils.log("MySQL backup and compression successful → " + zipPath);
                }

                return true;
            } else {
                System.out.println("Backup failed. Exit code: " + exitCode);
                return false;
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("Exception during backup: " + e.getMessage());
            return false;
        }
    }
    @Override
    public boolean fullRestore(String path) {
            System.out.println(" Restoring MySQL database from: " + path);
            String cmd = "mysql -u " + user + " -p" + password + " " + database + " < " + path;
            try {
                String[] fullCmd = {"/bin/sh", "-c", cmd};
                Process proc = Runtime.getRuntime().exec(fullCmd);
                int exitCode = proc.waitFor();

                if (exitCode == 0) {
                    System.out.println("MySQL restore completed.");
                    return true;
                } else {
                    System.out.println("Restore failed. Exit code: " + exitCode);
                    return false;
                }
            } catch (IOException | InterruptedException e) {
                System.out.println("Exception during MySQL restore: " + e.getMessage());
                return false;
            }
        }


    }



