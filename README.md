# DATABASE BACKUP UTILITY

A CLI-based utility for **full**, **incremental**, and **selective** backup and restore operations for **MySQL** and **MongoDB** databases. Built for resilience, compression, logging, and operational visibility.

---

## FEATURES

- Full Backup & Restore (MySQL / MongoDB)
- Incremental Backup & Restore:
  - MySQL via binary logs
  - MongoDB via CDC (Change Data Capture)
- Selective Restore (specific tables or collections)
- Compression enabled by default (.zip)
- Logs all operations to `logs/backup.log`
- Optional notification via `ntfy.sh`
- Supports both standalone and replica set MongoDB
- Interactive CLI — no flags required

---

## TECH STACK

- Java 21
- Maven
- Custom-built modules for:
  - MySQL binlog management
  - MongoDB CDC capture
  - Incremental restore
  - Notifications

---

## USAGE INSTRUCTIONS

### RUN THE UTILITY
To run the utility follow the following steps:
- Clone the project in to your system (NOTE: git must be installed in your system).
- Open the cloned project and open terminal there.
- Now allow the bash file to be executable by running.
```bash
sudo chmod +x start-utility.sh
```
- Then execute the bash script by running.
``` bash
./start-utility.sh
```

### LOGGING 
All Operations are logged to:
```
logs/backup.log
```

### NOTIFICATIONS
This utility uses ntfy.sh for backup completion alerts.
Users need to subscribe to ntfy.sh/BackupStatus to recieve backup related notifications.

### BACKUP SCHEDULING
This utility does not implement internal job scheduling.

To schedule recurring backups:
- Use cron on Linux/macOS.
- Use Task Scheduler on Windows.

### FINAL NOTES

- Compression is always enabled.
- MongoDB CDC only works if replica set is active.
- All backups are stored timestamped and zipped.
- Incremental restore available for MySQL and MongoDB.


Solved this problem : https://roadmap.sh/projects/database-backup-utility
