package com.backup;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.zip.*;

public class FileUtils {

    public static String createTimestampFolder(String dbType, String dbName) {
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String timestamp = new SimpleDateFormat("HHmmss").format(new Date());
        String folderName = dbType.toUpperCase() + "_" + dbName + "_" + timestamp;

        File baseDir = new File("backups" + File.separator + date);
        File backupDir = new File(baseDir, folderName);

        if (!backupDir.mkdirs()) {
            System.out.println(" Could not create folder: " + backupDir.getAbsolutePath());
        }

        return String.valueOf(backupDir);
    }

    public static boolean zipDirectory(String sourceDir, String outputZipPath) {
        try (
                FileOutputStream fos = new FileOutputStream(outputZipPath);
                ZipOutputStream zos = new ZipOutputStream(fos)
        ) {
            File dirToZip = new File(sourceDir);
            zipFile(dirToZip, dirToZip.getName(), zos);
            return true;
        } catch (IOException e) {
            System.out.println("Error while zipping: " + e.getMessage());
            return false;
        }
    }

    private static void zipFile(File fileToZip, String fileName, ZipOutputStream zos) throws IOException {
        if (fileToZip.isHidden()) return;

        if (fileToZip.isDirectory()) {
            if (!fileName.endsWith("/")) fileName += "/";
            zos.putNextEntry(new ZipEntry(fileName));
            zos.closeEntry();
            for (File child : Objects.requireNonNull(fileToZip.listFiles())) {
                zipFile(child, fileName + child.getName(), zos);
            }
            return;
        }

        try (FileInputStream fis = new FileInputStream(fileToZip)) {
            ZipEntry zipEntry = new ZipEntry(fileName);
            zos.putNextEntry(zipEntry);
            byte[] bytes = new byte[4096];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
        }
    }

    public static void log(String message) {
        try (FileWriter fw = new FileWriter("backup.log", true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            bw.write("[" + timestamp + "] " + message);
            bw.newLine();
        } catch (IOException e) {
            System.out.println("Could not write to log: " + e.getMessage());
        }
    }

    public static void sendNotification(String topic, String message) {
        try {
            URL url = new URL("https://ntfy.sh/" + topic);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            byte[] out = message.getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(out.length);
            conn.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");
            conn.connect();
            try (OutputStream os = conn.getOutputStream()) {
                os.write(out);
            }
            if (conn.getResponseCode() != 200) {
                System.err.println("ntfy.sh notification failed: " + conn.getResponseCode());
            }
        } catch (Exception e) {
            System.err.println("ntfy.sh notification error: " + e.getMessage());
        }
    }




}
