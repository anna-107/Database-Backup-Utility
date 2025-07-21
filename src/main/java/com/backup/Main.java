package com.backup;

import com.backup.service.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);

        System.out.println("===== DATABASE BACKUP UTILITY========");
        System.out.println("Welcome, operator.\n");
        System.out.println("Press 1 to start");
        System.out.println("Press 0 or type --help for assistance");


        String choice = scanner.nextLine().trim();
        if (choice.equals("0") || choice.equalsIgnoreCase("--help")) {
            CLIHandler.showHelp();
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
        System.out.println("\nTesting connection...\n");

        boolean ok = service.testConnection();
        if (ok) {
            System.out.println("\nConnection test succeeded.");
            System.out.println("Do you want to backup or restore?\n" +
                    "Press 1 for backup, press 0 for restore!");
            int choice2= Integer.parseInt(scanner.nextLine().trim());
            if(choice2==1){
                System.out.println("1 : Full Backup");
                System.out.println("2 : Incremental Backup");
                int mysqlChoice = Integer.parseInt(scanner.nextLine());
                if (mysqlChoice == 1) {
                    System.out.println("Full Backup proceeding...");
                      service.fullBackup();
                    } else if (mysqlChoice == 2) {
                        if(dbType.equals("mysql")){
                        System.out.println("Incremental backup via bin-logs selected.");
                        service.IncrementalBackup();

                    } else{
                            System.out.println("Incremental backup via MongoDB CDC initiating...");
                            service.IncrementalBackup();
                        }
                }

            }
            else{
               System.out.print("Enter full path to the backup folder/file: ");
                String restorePath = scanner.nextLine().trim();

                System.out.print("Do you want selective restore? (y/n): ");
                String isSelective = scanner.nextLine().trim().toLowerCase();

                if (isSelective.equals("y")) {
                    System.out.print("Enter comma-separated collection names to restore: ");
                    String colInput = scanner.nextLine().trim();
                    List<String> collections = Arrays.asList(colInput.split(","));

                    boolean ok1 = service.selectiveRestore(restorePath, collections);
                    System.out.println(ok1 ? "Selective restore complete." : "Restore had issues.");
                } else {
                    boolean ok1 = service.fullRestore(restorePath);
                    System.out.println(ok1 ? "Full restore complete." : "Restore failed.");
                    if(ok1){
                        System.out.println("Do you want Incremental Restore? (y/n): ");
                        String choice1 = scanner.nextLine();
                        if(choice1.equals("y")){
                            service.IncrementalRestore();
                        }
                        else{
                            System.out.println("Backup Complete! Aborting...");
                        }
                    }
                }

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
