package com.backup.service.mongodb;

import com.mongodb.ConnectionString;
import com.mongodb.client.*;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import org.bson.BsonTimestamp;
import org.bson.Document;
import org.bson.json.JsonWriterSettings;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

public class MongoIncrementalBackup {

    public static void captureChanges(String uri, String dbName, String outputPath) {
        MongoClient mongoClient = MongoClients.create(new ConnectionString(uri));
        MongoDatabase db = mongoClient.getDatabase(dbName);

        BsonTimestamp lastTs = MongoCDCTracker.readLastTimestamp();
        System.out.println("Watching for changes since: " + lastTs.getTime());

        String fileName = outputPath + "/mongo_cdc_" + System.currentTimeMillis() + ".json";

        try (MongoChangeStreamCursor<ChangeStreamDocument<Document>> cursor =
                     (MongoChangeStreamCursor<ChangeStreamDocument<Document>>) db.watch()
                             .fullDocument(FullDocument.UPDATE_LOOKUP)
                             .startAtOperationTime(lastTs)
                             .iterator();
             FileWriter writer = new FileWriter(fileName)) {

            while (cursor.hasNext()) {
                ChangeStreamDocument<Document> change = cursor.next();

                String json;
                if (change.getFullDocument() != null) {
                    json = change.getFullDocument().toJson(JsonWriterSettings.builder().indent(true).build());
                } else {
                    assert change.getOperationType() != null;
                    json = new Document("operation", change.getOperationType().getValue()).toJson();
                }

                writer.write(json + "\n");
                writer.flush(); // flush on every event to avoid loss

                assert change.getClusterTime() != null;
                MongoCDCTracker.writeTimestamp(change.getClusterTime());
                System.out.println("Captured: " + change.getOperationType());
            }

        } catch (IOException e) {
            System.out.println("Error writing CDC backup: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("CDC stream error: " + e.getMessage());
        } finally {
            mongoClient.close();
        }
    }
}
