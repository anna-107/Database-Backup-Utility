#!/bin/bash

# Navigate to the project directory
cd Database-Backup-Utility/ 

# Build the project using Maven
echo "Running Maven build..."
mvn clean package

# Run the Java application
echo "Starting the backup utility..."
java -jar target/db-backup-cli-1.0-SNAPSHOT-jar-with-dependencies.jar
