package com.letsplay.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class DotenvLoader {

    private DotenvLoader() {}

    public static void load() {
        for (String path : new String[]{".env", "../.env"}) {
            Path file = Paths.get(path);
            if (Files.exists(file)) {
                try (var lines = Files.lines(file)) {
                    lines.map(String::trim)
                            .filter(line -> !line.isEmpty() && !line.startsWith("#") && line.contains("="))
                            .forEach(line -> {
                                String[] parts = line.split("=", 2);
                                String key = parts[0].trim();
                                String value = parts[1].trim().replaceAll("^[\"']|[\"']$", "");
                                System.setProperty(key, value);
                            });
                    break;
                } catch (Exception ignored) {
                }
            }
        }
    }
}
