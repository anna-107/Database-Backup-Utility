package com.backup.service.mysql;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.Scanner;

import com.backup.FileUtils;
import org.json.simple.JSONObject;

public class BinlogIncrementalBackup {

    public static void performIncrementalBackup( String mysqlUser,  String mysqlPass, String mysqlHost, Integer mysqlPort, String mysqlLogDir, String outputPath) {
        try {
            // 1. Connect to DB and get current binlog position
            Connection conn = DriverManager.getConnection("jdbc:mysql://" + mysqlHost + ":" + mysqlPort + "/", mysqlUser, mysqlPass);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SHOW MASTER STATUS");

            if (!rs.next()) {
                System.out.println("Binlog not enabled or insufficient privileges.");
                return;
            }

            String currentFile = rs.getString("File");
            long currentPos = rs.getLong("Position");

            System.out.println("Current Binlog Position: " + currentFile + " @ " + currentPos);

            // 2. Read previous checkpoint
            JSONObject state = BackupTracker.readCheckpoint();
            String lastFile = (String) state.get("last_binlog_file");
            long lastPos = (long) state.get("last_position");

            System.out.println("Last Backup Position: " + lastFile + " @ " + lastPos);

            // 3. Build mysqlbinlog command
            String command = String.format(
                    "sudo mysqlbinlog --start-position=%d %s/%s > %s/mysql_incremental_%s.sql",
                    lastPos,
                    mysqlLogDir,
                    currentFile,
                    outputPath,
                    currentFile.replace(".","_")
            );

            // 4. Run command
            System.out.println("Executing: " + command);
            Process process = Runtime.getRuntime().exec(new String[] { "bash", "-c", command });
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.err.println(line);
            }

            process.waitFor();
            System.out.println("Incremental backup completed.");
            

            // 5. Save new checkpoint
            BackupTracker.writeCheckpoint(currentFile, currentPos);

        } catch (Exception e) {
            System.out.println("Error during incremental backup: " + e.getMessage());
        }
    }
}
