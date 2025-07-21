package com.backup.service.mysql;

import java.io.*;
import java.util.*;
import java.util.regex.*;

    public class MySQLDependencyResolver {

        private final Map<String, Set<String>> dependencyMap = new HashMap<>();

        public Map<String, Set<String>> getDependencyMap() {
            return dependencyMap;
        }

        public void buildDependencyGraph(String sqlFilePath) {
            try (BufferedReader reader = new BufferedReader(new FileReader(sqlFilePath))) {
                String line;
                String currentTable = null;
                boolean insideCreate = false;

                while ((line = reader.readLine()) != null) {
                    line = line.trim();

                    if (line.toUpperCase().startsWith("CREATE TABLE")) {
                        currentTable = extractTableName(line);
                        insideCreate = true;
                        dependencyMap.putIfAbsent(currentTable, new HashSet<>());
                    }

                    if (insideCreate && line.contains("FOREIGN KEY")) {
                        String referencedTable = extractReferencedTable(line);
                        if (referencedTable != null && currentTable != null) {
                            dependencyMap.get(currentTable).add(referencedTable);
                        }
                    }

                    if (insideCreate && line.endsWith(";")) {
                        insideCreate = false;
                        currentTable = null;
                    }
                }

            } catch (IOException e) {
                System.out.println("💥 Error reading SQL file for dependency analysis: " + e.getMessage());
            }
        }

        private String extractTableName(String createLine) {
            Pattern pattern = Pattern.compile("CREATE TABLE [`\"]?(\\w+)[`\"]?", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(createLine);
            if (matcher.find()) {
                return matcher.group(1);
            }
            return null;
        }

        private String extractReferencedTable(String line) {
            Pattern pattern = Pattern.compile("REFERENCES [`\"]?(\\w+)[`\"]?", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                return matcher.group(1);
            }
            return null;
        }

        public Set<String> resolveWithDependencies(Set<String> selectedTables) {
            Set<String> finalSet = new HashSet<>(selectedTables);
            Queue<String> queue = new LinkedList<>(selectedTables);

            while (!queue.isEmpty()) {
                String table = queue.poll();
                Set<String> deps = dependencyMap.getOrDefault(table, Collections.emptySet());

                for (String dep : deps) {
                    if (!finalSet.contains(dep)) {
                        finalSet.add(dep);
                        queue.add(dep);
                    }
                }
            }

            return finalSet;
        }
    }

