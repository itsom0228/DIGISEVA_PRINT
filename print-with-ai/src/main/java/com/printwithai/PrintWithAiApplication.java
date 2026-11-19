package com.printwithai;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;

@SpringBootApplication
@EnableScheduling
public class PrintWithAiApplication {

    public static void main(String[] args) {
        loadDotenv();
        String os = System.getProperty("os.name", "").toLowerCase();
        boolean isWindows = os.contains("win");
        // In cloud / Docker Linux deployments (Render, Alpine), headless must be true to prevent X11 server errors.
        // On Windows, headless is false for OS print/desktop viewer integration.
        System.setProperty("java.awt.headless", isWindows ? "false" : "true");
        new SpringApplicationBuilder(PrintWithAiApplication.class)
                .headless(!isWindows)
                .run(args);
    }

    private static void loadDotenv() {
        File[] possibleFiles = new File[]{ new File(".env"), new File("../.env"), new File("print-with-ai/.env") };
        for (File envFile : possibleFiles) {
            if (envFile.exists() && envFile.isFile()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(envFile, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                            continue;
                        }
                        int eq = line.indexOf('=');
                        String key = line.substring(0, eq).trim();
                        String value = line.substring(eq + 1).trim();
                        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                            value = value.substring(1, value.length() - 1);
                        }
                        if (System.getProperty(key) == null && System.getenv(key) == null) {
                            System.setProperty(key, value);
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }
}
