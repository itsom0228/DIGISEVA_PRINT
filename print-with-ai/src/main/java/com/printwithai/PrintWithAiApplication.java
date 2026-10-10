package com.printwithai;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PrintWithAiApplication {

    public static void main(String[] args) {
        String os = System.getProperty("os.name", "").toLowerCase();
        boolean isWindows = os.contains("win");
        // In cloud / Docker Linux deployments (Render, Alpine), headless must be true to prevent X11 server errors.
        // On Windows, headless is false for OS print/desktop viewer integration.
        System.setProperty("java.awt.headless", isWindows ? "false" : "true");
        new SpringApplicationBuilder(PrintWithAiApplication.class)
                .headless(!isWindows)
                .run(args);
    }
}
