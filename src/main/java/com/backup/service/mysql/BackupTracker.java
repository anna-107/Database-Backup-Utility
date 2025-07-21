package com.backup.service.mysql;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

@SuppressWarnings("ALL")
public class BackupTracker {

    private static final String TRACKER_PATH = "BinLogs/mysql-binlog-checkpoint.json";

    public static JSONObject readCheckpoint() {
        File file = new File(TRACKER_PATH);
        if (!file.exists()) {
            JSONObject defaultState = new JSONObject();
            defaultState.put("last_binlog_file", "");
            defaultState.put("last_position", 4L);
            return defaultState;
        }

        try (FileReader reader = new FileReader(file)) {
            JSONParser parser = new JSONParser();
            return (JSONObject) parser.parse(reader);
        } catch (Exception e) {
            System.out.println("Error reading checkpoint: " + e.getMessage());
            return null;
        }
    }

    public static void writeCheckpoint(String binlogFile, long position) {
        JSONObject state = new JSONObject();
        state.put("last_binlog_file", binlogFile);
        state.put("last_position", position);

        try (FileWriter writer = new FileWriter(TRACKER_PATH)) {
            writer.append(state.toJSONString());
            System.out.println("Written next BinLog in "+ TRACKER_PATH);
        } catch (IOException e) {
            System.out.println("Error writing checkpoint: " + e.getMessage());
        }
    }
}
