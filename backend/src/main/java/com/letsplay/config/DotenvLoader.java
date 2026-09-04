package com.letsplay.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Lightweight helper to load .env configuration variables into System properties
 * at application and test startup without requiring Spring SPI / META-INF.
 */
public final class DotenvLoader {

    private DotenvLoader() {}

    public static void load() {
        File[] candidateFiles = new File[]{
                new File(".env"),
                new File("../.env"),
                new File(System.getProperty("user.dir"), ".env"),
                new File(System.getProperty("user.dir"), "../.env")
        };

        for (File file : candidateFiles) {
            if (file.exists() && file.isFile()) {
                loadFile(file);
                break;
            }
        }
    }

    private static void loadFile(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eqIdx = line.indexOf('=');
                if (eqIdx > 0) {
                    String key = line.substring(0, eqIdx).trim();
                    String val = line.substring(eqIdx + 1).trim();
                    if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                        if (val.length() >= 2) {
                            val = val.substring(1, val.length() - 1);
                        }
                    }
                    if (System.getProperty(key) == null && System.getenv(key) == null) {
                        System.setProperty(key, val);
                    }
                    if (key.equalsIgnoreCase("JWT_SECRET") && System.getProperty("jwt.secret") == null) {
                        System.setProperty("jwt.secret", val);
                    } else if (key.equalsIgnoreCase("JWT_EXPIRATION") && System.getProperty("jwt.expiration") == null) {
                        System.setProperty("jwt.expiration", val);
                    } else if (key.equalsIgnoreCase("SERVER_PORT") && System.getProperty("server.port") == null) {
                        System.setProperty("server.port", val);
                    } else if (key.equalsIgnoreCase("SPRING_DATA_MONGODB_HOST") && System.getProperty("spring.data.mongodb.host") == null) {
                        System.setProperty("spring.data.mongodb.host", val);
                    } else if (key.equalsIgnoreCase("SPRING_DATA_MONGODB_PORT") && System.getProperty("spring.data.mongodb.port") == null) {
                        System.setProperty("spring.data.mongodb.port", val);
                    } else if (key.equalsIgnoreCase("SPRING_DATA_MONGODB_DATABASE") && System.getProperty("spring.data.mongodb.database") == null) {
                        System.setProperty("spring.data.mongodb.database", val);
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }
}
