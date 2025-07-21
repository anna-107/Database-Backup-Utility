package com.backup;

import java.util.Scanner;

public class CLIHandler {
    public static void showHelp() {
        System.out.println("""
╔══════════════════════════════════════════════════════════════╗
║           DATABASE BACKUP UTILITY - USER GUIDE               ║
╚══════════════════════════════════════════════════════════════╝

HOW THIS WORKS:

➊ On launch, you’ll be asked:
   ▸ Press [1] to START
   ▸ Press [0] or type [--help] for this guide

➋ Next, choose your database type:
   ▸ [1] MySQL
   ▸ [2] MongoDB

➌ Provide credentials:
   ▸ Host, Port, Username, Password, DB Name

➍ Choose operation:
   ▸ [1] Full Backup
   ▸ [2] Incremental Backup
   ▸ [3] Full Restore
   ▸ [4] Incremental Restore
   ▸ [5] Selective Restore (tables/collections)

➎ For MongoDB CDC:
   ▸ Select “Incremental Backup”
   ▸ MongoDB must run as Replica Set
   ▸ Captured CDC logs are saved in /backups/

➏ For Restore:
   ▸ Select the backup file when prompted
   ▸ You can restore entire DB or specific parts

➐ Optional:
   ▸ Notifications are sent via ntfy.sh to topic “Backup_complete”
   ▸ Logs are saved in /logs/backup.log

 NOTE:
   ▸ Backups are compressed by default (.zip)
   ▸ Schedule backups using OS-level cron jobs (see README)
   ▸ Always test connection before proceeding

╔═════════════════════════════════════════════════════════════════╗
║   Press ENTER to go back! You need to re-run the script now!    ║
╚═════════════════════════════════════════════════════════════════╝
    """);

        new Scanner(System.in).nextLine(); // pause before returning
    }

}
