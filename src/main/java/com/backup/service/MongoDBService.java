package com.backup.service;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class MongoDBService implements DatabaseService {

    private String url;
    private String databaseName;
    private String host;
    private int port;
    private String user;
    private String password;
    private boolean isCloud;


    public MongoDBService() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("""
                 Is Locally hosted or cloud hosted?\s
                 Press 0 for Locally hosted and 1 for cloud hosted:
                \s""");
        int hostedWhere = Integer.parseInt(scanner.nextLine().trim());
        if (hostedWhere == 1) {
            isCloud = true;
            System.out.print("Enter uri: ");
            this.url = scanner.nextLine().trim();
            System.out.print("Enter DataBase Name: ");
            this.databaseName = scanner.nextLine().trim();
        } else if (hostedWhere == 0) {
            isCloud = false;
            System.out.print("Enter Host: ");
            this.host = scanner.nextLine().trim();
            System.out.print("Enter Port: ");
            this.port = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("Enter Username: ");
            this.user = scanner.nextLine().trim();
            System.out.print("Enter Password: ");
            this.password = scanner.nextLine().trim();
            System.out.print("Enter Database Name: ");
            this.databaseName = scanner.nextLine().trim();
        } else {
            System.out.println("Invalid Selection!! Aborting!");
            throw new IllegalArgumentException("Invalid selection");
        }
    }

    @Override
    public boolean testConnection() {
        String Connectionuri;
        if (isCloud) {
            Connectionuri = url;
            System.out.println("FYI your data might be already backed up by Atlas! Anyways moving ahead!");
        } else {

            Connectionuri = "mongodb://" + user + ":" + password + "@" + host + ":" + port + "/";
        }
        try (MongoClient client = MongoClients.create(Connectionuri)) {
            MongoDatabase db = client.getDatabase(databaseName);
            db.listCollectionNames().first();          // checking connection
            Document ping = new Document("ping", 1);
            Document result = db.runCommand(ping);
            System.out.println("Ping result: " + result.toJson());
            System.out.println("MongoDB connection established.");
            return true;
        } catch (Exception e) {
            System.out.println("MongoDB connection failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean fullBackup() {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String backupDir  = "backups";
            new File(backupDir).mkdirs();

            // mongodump writes into a directory; we’ll name it database_timestamp
            String dumpFolder = backupDir + File.separator + databaseName + "_dump_" + timestamp;

            String cmd = String.format(
                    "mongodump --db %s --username %s --password %s --authenticationDatabase admin --out %s",
                    databaseName, user, password, dumpFolder
            );

            System.out.println("Running MongoDB backup → " + dumpFolder);

            try {
                Process proc = Runtime.getRuntime().exec(cmd);
                int exitCode = proc.waitFor();

                if (exitCode == 0) {
                    System.out.println("MongoDB backup completed → " + dumpFolder);
                    return true;
                } else {
                    System.out.println("mongodump failed. Exit code: " + exitCode);
                    return false;
                }
            } catch (InterruptedException | IOException e) {
                System.out.println("Exception during backup: " + e.getMessage());
                return false;
            }
        }

    @Override
    public boolean fullRestore(String path) {
            System.out.println(" Restoring MongoDB database from: " + path);
            String cmd = String.format(
                    "mongorestore --db %s %s",
                    databaseName, path
            );

            try {
                Process proc = Runtime.getRuntime().exec(cmd);
                int exitCode = proc.waitFor();

                if (exitCode == 0) {
                    System.out.println("MongoDB restore completed.");
                    return true;
                } else {
                    System.out.println("Restore failed. Exit code: " + exitCode);
                    return false;
                }
            } catch (IOException | InterruptedException e) {
                System.out.println("Exception during MongoDB restore: " + e.getMessage());
                return false;
            }
        }




}
