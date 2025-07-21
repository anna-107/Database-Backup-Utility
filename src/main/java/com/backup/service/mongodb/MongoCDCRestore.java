package com.backup.service.mongodb;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;

public class MongoCDCRestore {

    public static void restoreFromCDCFile(String uri, String cdcFilePath) {

        try (MongoClient mongoClient = MongoClients.create(new ConnectionString(uri)); BufferedReader reader = new BufferedReader(new FileReader(cdcFilePath))) {
            String line;
            int lineNum = 0;

            while ((line = reader.readLine()) != null) {
                lineNum++;
                try {
                    JSONObject json = (JSONObject) new JSONParser().parse(line);

                    String operation = (String) json.get("operationType");
                    JSONObject ns = (JSONObject) json.get("ns");
                    if (ns == null) continue;

                    String dbName = (String) ns.get("db");
                    String collName = (String) ns.get("coll");
                    if (dbName == null || collName == null) continue;

                    MongoDatabase db = mongoClient.getDatabase(dbName);
                    MongoCollection<Document> collection = db.getCollection(collName);

                    switch (operation) {
                        case "insert":
                        case "replace":
                        case "update":
                            JSONObject fullDoc = (JSONObject) json.get("fullDocument");
                            if (fullDoc != null) {
                                Document doc = Document.parse(fullDoc.toJSONString());
                                collection.insertOne(doc);
                            }
                            break;

                        case "delete":
                            JSONObject key = (JSONObject) json.get("documentKey");
                            if (key != null) {
                                Document delQuery = Document.parse(key.toJSONString());
                                collection.deleteOne(delQuery);
                            }
                            break;

                        default:
                            System.out.println("Unknown opType on line " + lineNum);
                    }

                } catch (Exception e) {
                    System.out.println("Error parsing line " + lineNum + ": " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.out.println("Failed to read CDC backup file: " + e.getMessage());
        }
    }
}
