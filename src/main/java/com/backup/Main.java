package com.backup;

import com.backup.service.*;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("===== DATABASE BACKUP UTILITY========");
        System.out.println("Welcome, operator.\n");
        System.out.println("Press 1 to start");
        System.out.println("Press 0 or type --help for assistance");

        String choice = scanner.nextLine().trim();
        if (choice.equals("0") || choice.equalsIgnoreCase("--help")) {
            showHelp();
            return;
        }
        if (!choice.equals("1")) {
            System.out.println(" Invalid option. Exiting.");
            return;
        }

     // ── Interactive credential collection ──
        System.out.print("\nEnter Database Type (mysql/mongodb): ");
        String dbType = scanner.nextLine().trim().toLowerCase();


        DatabaseService service = switch (dbType) {
            case "mysql"   -> new MySQLService();
            case "mongodb" -> new MongoDBService();
            default        -> null;
        };

        if (service == null) {
            System.out.println("Unsupported DB type.");
            return;
        }
        System.out.println("\n🔍 Testing connection...\n");

        boolean ok = service.testConnection();
        if (ok) {
            System.out.println("\nConnection test succeeded.");
            System.out.println("Do you want to backup or restore?\n" +
                    "Press 1 for backup, press 0 for restore!");
            int choice2= Integer.parseInt(scanner.nextLine().trim());
            if(choice2==1){
            service.fullBackup();}
            else{
                System.out.println("Enter the path of your backup file: ");
                String path=scanner.nextLine().trim();
                service.fullRestore(path);
            }

        } else {
            System.out.println("\nConnection test failed. Fix credentials/network and retry.");
        }
    }

    private static void showHelp() {
        System.out.println("\n================= HELP MENU =================");
        System.out.println("1  → start interactive setup");
        System.out.println("0  → show this menu / exit");
        System.out.println("The tool currently checks connectivity to MySQL or MongoDB. It can create a backup of your database!");
    }
}
