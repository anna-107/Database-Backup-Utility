package com.backup.service;

public interface DatabaseService {

    boolean testConnection();

    boolean fullBackup();

    boolean fullRestore(String path);

    // later: incrementalBackup(), diffBackup(), etc.
}
