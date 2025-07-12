package com.backup.service;

import java.util.Scanner;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class MongoDBService implements DatabaseService {

    private  String url;
    private  String databaseName;
    private  String host;
    private  int port;
    private  String user;
    private  String password;
    private  boolean isCloud;


    public MongoDBService() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("""
                 Is Locally hosted or cloud hosted?\s
                 Press 0 for Locally hosted and 1 for cloud hosted:
                \s""");
        int hostedWhere = Integer.parseInt(scanner.nextLine().trim());
        if(hostedWhere==1){
                isCloud=true;
                System.out.print("Enter uri: ");
                this.url = scanner.nextLine().trim();
                System.out.print("Enter DataBase Name: ");
                this.databaseName = scanner.nextLine().trim();
            }
            else if( hostedWhere==0){
                isCloud=false;
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
            }
            else{
                System.out.println("Invalid Selection!! Aborting!");
                throw  new IllegalArgumentException("Invalid selection");
            }
        }
    


    @Override
    public boolean testConnection() {
        String Connectionuri;
        if (isCloud){
            Connectionuri= url;
        }else{

        Connectionuri= "mongodb://"+user+":"+password+"@"+host+":"+port+"/";
        }
        try (MongoClient client = MongoClients.create(Connectionuri)) {
            MongoDatabase db = client.getDatabase(databaseName);
            db.listCollectionNames().first();          // lightweight ping
            System.out.println(" MongoDB connection established.");
            return true;
        } catch (Exception e) {
            System.out.println(" MongoDB connection failed: " + e.getMessage());
            return false;
        }
    }
}
