package com.backup.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.backup.service.mongodb.MongoCDCRestore;
import com.backup.service.mongodb.MongoIncrementalBackup;
import org.bson.Document;

import com.backup.FileUtils;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class MongoDBService implements DatabaseService {

    private String url;
    private String databaseName;
    private String host;
    private int port;
    private String user;
    private String password;
    private boolean isCloud;
    private String Connectionuri;
    private String response;


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

        if (isCloud) {
            Connectionuri = url;
            System.out.println("FYI your data might be already backed up by Atlas! Anyways moving ahead!");
        } else {

            Connectionuri = "mongodb://" + user + ":" + password + "@" + host + ":" + port + "/?authSource=admin";
        }
        try (MongoClient client = MongoClients.create(Connectionuri)) {
            System.out.println("Connected! First Database is:  " + client.listDatabaseNames().first());
            return true;
        } catch (Exception e) {
            System.out.println("MongoDB connection failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean fullBackup() {
        Scanner scanner = new Scanner(System.in);
        String cmd;
        String backupDir = FileUtils.createTimestampFolder("mongodb", databaseName);
        String dumpFolder = backupDir + File.separator + "dump";// mongodump writes into a directory; we’ll name it database_timestamp
        System.out.println("Is this MongoDB a replica set? (y/n): ");
        response = scanner.nextLine().trim().toLowerCase();
        if (response.equals("n")) {
            cmd = String.format(
                    "mongodump --host=%s --port=%d --username=%s --password=%s --authenticationDatabase=admin --db=%s --out=%s",
                    host, port, user, password, databaseName, dumpFolder
            );
        }
        else{
            cmd = String.format(
                    "mongodump --host=localhost --port=27017 --out=%s --oplog",
                    dumpFolder
            );
        }
            System.out.println("Running MongoDB backup → " + dumpFolder);
           try {
                Process proc = Runtime.getRuntime().exec(cmd);
                int exitCode = proc.waitFor();

                if (exitCode == 0) {
                    System.out.println("MongoDB backup completed → " + dumpFolder);
                    String zipPath = backupDir + ".zip";
                    if (FileUtils.zipDirectory(backupDir, zipPath)) {
                        System.out.println("Backup zipped → " + zipPath);
                        FileUtils.log("MongoDB backup successful → " + zipPath);
                    }
                    FileUtils.sendNotification("Backup_complete", "Backup of "+databaseName+" completed Successfully!");
                    return true;
                } else {
                    System.out.println("mongodump failed. Exit code: " + exitCode);
                    FileUtils.log("MongoDB backup failed with exit code: " + exitCode);
                    return false;
                }
            } catch (InterruptedException | IOException e) {
                System.out.println("Exception during backup: " + e.getMessage());
                return false;
            }

        }


    @Override
    public boolean fullRestore(String path) {
        String cmd;
        Scanner scanner= new Scanner(System.in);
        System.out.println("Is this MongoDB a replica set? (y/n): ");
        response = scanner.nextLine().trim().toLowerCase();
        if (response.equals("n")) {
            cmd = String.format(
                    "mongorestore --db %s %s",
                    databaseName, path
            );
        }
        else{
            cmd = String.format(
                    "mongorestore --dir %s --oplogReplay",path
            );
        }

        System.out.println("Restoring MongoDB database from: " + path);


        try {
            Process proc = Runtime.getRuntime().exec(cmd);
            int exitCode = proc.waitFor();

            String msg = String.format(
                    "MongoDB restore %s from %s → DB: %s",
                    (exitCode == 0 ? "successful" : "failed"),
                    path,
                    databaseName
            );

            FileUtils.log(msg);
            System.out.println(msg);
            FileUtils.sendNotification("RestoreStatus", "Restoration of "+databaseName+" completed Successfully!");

            return exitCode == 0;

        } catch (IOException | InterruptedException e) {
            String err = "MongoDB restore exception: " + e.getMessage();
            FileUtils.log(err + " (from: " + path + " → DB: " + databaseName + ")");
            System.out.println(err);
            return false;
        }
    }

    private Set<String> detectLinkedCollections(File dbDir, List<String> selected) {
        Set<String> suggestions = new HashSet<>();

        for (String sel : selected) {
            File jsonDump = new File(dbDir, sel + ".metadata.json");
            File bsonDump = new File(dbDir, sel + ".bson");

            try (BufferedReader br = new BufferedReader(new FileReader(bsonDump))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.contains("$ref")) {
                        Matcher m = Pattern.compile("\"\\$ref\"\\s*:\\s*\"(\\w+)\"").matcher(line);
                        while (m.find()) {
                            suggestions.add(m.group(1));
                        }
                    }

                    Matcher refFields = Pattern.compile("\"(\\w+_id)\"\\s*:\\s*\\{?\\s*\\$?o?b?j?e?c?t?I?d?\\s*").matcher(line);
                    while (refFields.find()) {
                        String field = refFields.group(1);
                        String guess = field.replace("_id", "");
                        suggestions.add(guess);
                    }
                }
            } catch (IOException e) {
                // Ignore unreadable files
            }
        }

        return suggestions;
    }


    @Override
    public boolean selectiveRestore(String path, List<String> collections) {
        System.out.println("Starting MongoDB selective restore...");

        // Step 1: Validate base path
        File baseDir = new File(path);
        if (!baseDir.exists() || !baseDir.isDirectory()) {
            System.out.println("Invalid backup folder: " + path);
            return false;
        }

        File dbDir = new File(baseDir, databaseName); // e.g., path/demo/
        if (!dbDir.exists() || !dbDir.isDirectory()) {
            System.out.println("Database folder not found: " + dbDir.getAbsolutePath());
            return false;
        }

        // Step 2: Detect available .bson files
        File[] bsonFiles = dbDir.listFiles((d, name) -> name.endsWith(".bson"));
        Set<String> availableCollections = new HashSet<>();
        if (bsonFiles != null) {
            for (File f : bsonFiles) {
                availableCollections.add(f.getName().replace(".bson", ""));
            }
        }

        // Step 3: Validate requested collections
        List<String> finalCollections = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String c : collections) {
            if (availableCollections.contains(c.trim())) {
                finalCollections.add(c.trim());
            } else {
                missing.add(c.trim());
            }
        }

        if (!missing.isEmpty()) {
            System.out.println("These collections were NOT found in the backup: " + missing);
            if (finalCollections.isEmpty()) {
                System.out.println("❌ No valid collections to restore.");
                return false;
            }
        }

        // Step 4: Try to detect linked collections
        Set<String> suggested = detectLinkedCollections(dbDir, finalCollections);
        finalCollections.forEach(suggested::remove);  // exclude already selected

        if (!suggested.isEmpty()) {
            System.out.println("These collections appear linked to your selection: " + suggested);
            System.out.print("Include them too? (y/n): ");
            String include = new Scanner(System.in).nextLine().trim().toLowerCase();
            if (include.equals("y")) {
                for (String s : suggested) {
                    if (availableCollections.contains(s)) finalCollections.add(s);
                }
            } else {
                System.out.println("Proceeding without linked collections.");
            }
        }

        // Step 5: Restore each collection
        boolean success = true;
        for (String col : finalCollections) {
            String bsonPath = dbDir.getAbsolutePath() + File.separator + col + ".bson";
            File f = new File(bsonPath);
            if (!f.exists()) {
                System.out.println("File missing for collection: " + col);
                success = false;
                continue;
            }

            String cmd = String.format(
                    "mongorestore --host %s --port %d --username %s --password %s --db %s --collection %s %s",
                    host, port, user, password, databaseName, col, bsonPath
            );

            try {
                Process proc = Runtime.getRuntime().exec(cmd);
                int exit = proc.waitFor();

                if (exit == 0) {
                    System.out.println("Restored collection: " + col);
                    FileUtils.log("MongoDB selective restore → " + col + " from " + bsonPath);
                } else {
                    System.out.println("Failed to restore: " + col);
                    success = false;
                }
            } catch (IOException | InterruptedException e) {
                System.out.println("Exception restoring " + col + ": " + e.getMessage());
                success = false;
            }
        }

        return success;
    }

    @Override
    public void IncrementalBackup() {
        String outputPath= FileUtils.createTimestampFolder("mongodb", databaseName);
        MongoIncrementalBackup.captureChanges(Connectionuri, databaseName, outputPath);

    }
    @Override
    public  void IncrementalRestore(){
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the path of CDC File: ");
        String path= scanner.nextLine().trim();
        MongoCDCRestore.restoreFromCDCFile(Connectionuri,path);
    }
}
