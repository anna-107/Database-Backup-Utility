package com.backup.service.mongodb;

import org.bson.BsonTimestamp;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;

@SuppressWarnings("unchecked")
public class MongoCDCTracker {
    private static final String TRACKER_FILE = "checkpoints/mongo-cdc-checkpoint.json";

    public static BsonTimestamp readLastTimestamp() {
        File file = new File(TRACKER_FILE);
        if (!file.exists()) {
            return new BsonTimestamp((int) (System.currentTimeMillis() / 1000), 0); // start from now
        }
        try (FileReader reader = new FileReader(file)) {
            JSONObject obj = (JSONObject) new JSONParser().parse(reader);
            int seconds = ((Long) obj.get("timestamp")).intValue();
            return new BsonTimestamp(seconds, 0);
        } catch (Exception e) {
            System.out.println("Error reading CDC checkpoint: " + e.getMessage());
            return new BsonTimestamp((int) (System.currentTimeMillis() / 1000), 0);
        }
    }

    public static void writeTimestamp(BsonTimestamp ts) {
        JSONObject obj = new JSONObject();
        obj.put("timestamp", ts.getTime());
        try (FileWriter writer = new FileWriter(TRACKER_FILE)) {
            writer.write(obj.toJSONString());
        } catch (IOException e) {
            System.out.println("Error writing CDC checkpoint: " + e.getMessage());
        }
    }
}
