package com.backup.service;

public interface DatabaseService {

    boolean testConnection();

   // boolean fullBackup();

  //  boolean restore(String backupPath);

    // later: incrementalBackup(), diffBackup(), etc.
}
