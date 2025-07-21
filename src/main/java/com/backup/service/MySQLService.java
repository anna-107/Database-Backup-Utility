package com.backup.service;

import com.backup.FileUtils;
import com.backup.service.mysql.BinlogIncrementalBackup;

import java.io.*;
import java.sql.*;
import java.util.*;

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
        this.port = Integer.parseInt(scanner.nextLine().trim());
        ;
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
                FileUtils.sendNotification("Backup_complete", "Backup of "+database+" completed Successfully!");
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

            String msg = String.format(
                    "MySQL restore %s from %s → DB: %s",
                    (exitCode == 0 ? "successful" : "failed"),
                    path,
                    database
            );

            FileUtils.log(msg);
            System.out.println(msg);
            FileUtils.sendNotification("RestoreStatus", "Restoration of "+database+" completed Successfully!");

            return exitCode == 0;

        } catch (IOException | InterruptedException e) {
            String err = "MySQL restore exception: " + e.getMessage();
            FileUtils.log(err + " (from: " + path + " → DB: " + database + ")");
            System.out.println(err);
            return false;
        }
    }

    /**
     * Dependency–aware selective restore.
     *
     * @param dumpPath full path to the master .sql dump
     * @param tables   user‑requested table list
     */
    @Override
    public boolean selectiveRestore(String dumpPath, List<String> tables) throws IOException {

        // ---------- 1.  Build full dependency set from INFORMATION_SCHEMA ----------
        Set<String> allTables = new LinkedHashSet<>();          // preserves insertion order
        Queue<String> queue = new ArrayDeque<>();

        tables.forEach(t -> {
            allTables.add(t.trim());
            queue.add(t.trim());
        });

        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://" + host + ":" + port + "/" + database +
                        "?useSSL=false&serverTimezone=UTC", user, password)) {
            String q =
                    "SELECT TABLE_NAME, REFERENCED_TABLE_NAME " +
                            "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND REFERENCED_TABLE_NAME IS NOT NULL";
            Map<String, Set<String>> fkMap = new HashMap<>();

            try (PreparedStatement ps = conn.prepareStatement(q);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    fkMap
                            .computeIfAbsent(rs.getString(1), k -> new HashSet<>())
                            .add(rs.getString(2));
                }
            }

            // BFS to collect every ancestor
            while (!queue.isEmpty()) {
                String tbl = queue.poll();
                for (String parent : fkMap.getOrDefault(tbl, Set.of())) {
                    if (allTables.add(parent)) queue.add(parent);
                }
            }

        } catch (SQLException e) {
            System.out.println("💥 Could not analyse FK dependencies: " + e.getMessage());
            return false;
        }

        // ---------- 2.  Prompt user if new dependencies appeared ----------
        Set<String> requested = new HashSet<>(tables);
        Set<String> extra = new HashSet<>(allTables);
        extra.removeAll(requested);

        if (!extra.isEmpty()) {
            System.out.println("Detected parent tables: " + extra);
            System.out.print("Include them in restore? (y/n): ");
            if (!new Scanner(System.in).nextLine().trim().equalsIgnoreCase("y")) {
                allTables.retainAll(requested);          // strip deps, may break FKs
                System.out.println("Proceeding without parents; FK errors possible.");
            }
        }

        // ---------- 3.  Filter dump for chosen tables ----------
        StringBuilder out = new StringBuilder("SET foreign_key_checks = 0;\n");
        try (BufferedReader br = new BufferedReader(new FileReader(dumpPath))) {
            String line;
            boolean capture = false;
            while ((line = br.readLine()) != null) {

                if (line.toUpperCase().startsWith("CREATE TABLE")
                        || line.toUpperCase().startsWith("DROP TABLE")) {
                    capture = false;
                    for (String tbl : allTables)
                        if (line.contains("`" + tbl + "`")) {
                            capture = true;
                            break;
                        }
                }

                if (line.startsWith("INSERT INTO")) {
                    capture = false;
                    for (String tbl : allTables)
                        if (line.contains("`" + tbl + "`")) {
                            capture = true;
                            break;
                        }
                }

                if (capture) out.append(line).append('\n');
            }
        } catch (IOException io) {
            System.out.println("💥 Could not scan dump: " + io.getMessage());
            return false;
        }
        out.append("SET foreign_key_checks = 1;\n");

        // ---------- 4.  Write temp SQL file ----------
        File tmp = File.createTempFile("mysql_selective_", ".sql");
        tmp.deleteOnExit();
        try (FileWriter fw = new FileWriter(tmp)) {
            fw.write(out.toString());
        }

        // ---------- 5.  Execute restore ----------
        List<String> cmd = List.of(
                "mysql",
                "-h", host,
                "-P", String.valueOf(port),
                "-u", user,
                "-p" + password,                  // TODO: secure
                database
        );

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectInput(tmp);
        pb.redirectErrorStream(true);

        try {
            Process p = pb.start();
            String err = new String(p.getInputStream().readAllBytes());
            if (p.waitFor() != 0) {
                System.out.println("Restore error:\n" + err);
                FileUtils.log("MySQL selective restore failed: " + err);
                return false;
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("Restore execution failed: " + e.getMessage());
            return false;
        }

        System.out.println("Selective restore completed for tables: " + allTables);
        FileUtils.log("MySQL selective restore: " + allTables + " from " + dumpPath);
        return true;
    }

    @Override
    public void IncrementalBackup(){
        Scanner scanner= new Scanner(System.in);
        System.out.print("Enter binlog directory (default: /var/log/mysql): ");
        String mysqlLogDir = scanner.nextLine();
        if (mysqlLogDir.isEmpty()) mysqlLogDir = "/var/log/mysql";

        String outputPath= FileUtils.createTimestampFolder("mysql", database);

        BinlogIncrementalBackup.performIncrementalBackup(user, password,host,port,mysqlLogDir, outputPath);
        FileUtils.sendNotification("Backup_complete", "Incremental Backup of "+database+" completed Successfully!");


    }

    @Override
    public void IncrementalRestore() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the path of Incremented Backup File: ");
        String path = scanner.nextLine().trim();
        String cmd = "mysql -u " + user + " -p" + password + " " + database + " < " + path;
        try {
            String[] fullCmd = {"/bin/sh", "-c", cmd};
            Process proc = Runtime.getRuntime().exec(fullCmd);
            int exitCode = proc.waitFor();

            String msg = String.format(
                    "MySQL incremental restore %s from %s → DB: %s",
                    (exitCode == 0 ? "successful" : "failed"),
                    path,
                    database
            );

        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}