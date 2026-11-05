package com.quiz.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Loads optional settings from config.properties (in the working directory).
 * Every setting has a safe default so the app runs even if the file is missing.
 */
public final class AppConfig {

    private static final Properties PROPS = new Properties();

    static {
        Path p = Paths.get("config.properties");
        if (Files.exists(p)) {
            try (InputStream in = Files.newInputStream(p)) {
                PROPS.load(in);
            } catch (IOException e) {
                System.err.println("Could not read config.properties, using defaults: " + e.getMessage());
            }
        }
    }

    private AppConfig() {
    }

    public static String get(String key, String def) {
        String v = System.getProperty(key, PROPS.getProperty(key));
        return v == null || v.trim().isEmpty() ? def : v.trim();
    }

    public static double getDouble(String key, double def) {
        try {
            return Double.parseDouble(get(key, String.valueOf(def)));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /** Path of the SQLite database file. */
    public static String dbPath() {
        return get("db.path", "data/quiz.db");
    }

    /** Marks awarded for a correct answer. */
    public static double marksCorrect() {
        return getDouble("marks.correct", 1.0);
    }

    /** Marks deducted for a wrong answer (0 = no negative marking). Unanswered never loses marks. */
    public static double marksWrong() {
        return getDouble("marks.wrong", 0.0);
    }
}
