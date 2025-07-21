package com.backup.service;

import java.io.IOException;
import java.util.List;

public interface DatabaseService {

    boolean testConnection();

    boolean fullBackup();

    boolean fullRestore(String path);

    boolean selectiveRestore(String path, List<String> collections) throws IOException;

    void IncrementalBackup();

    void IncrementalRestore();
}
