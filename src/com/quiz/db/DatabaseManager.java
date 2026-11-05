package com.quiz.db;

import com.quiz.exception.DataAccessException;
import com.quiz.util.AppConfig;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Owns the JDBC connection settings and the database schema.
 * SQLite stores the whole database in one file (data/quiz.db by default).
 */
public final class DatabaseManager {

    private static DatabaseManager instance;

    private final String url;

    private DatabaseManager(String dbPath) {
        File f = new File(dbPath);
        File parent = f.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        this.url = "jdbc:sqlite:" + f.getPath();
    }

    /** Singleton accessor using the configured database path. */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager(AppConfig.dbPath());
        }
        return instance;
    }

    /** Used by tests to point at a throwaway database file. */
    public static synchronized DatabaseManager useDatabase(String dbPath) {
        instance = new DatabaseManager(dbPath);
        return instance;
    }

    /** Opens a new connection. Callers must close it (use try-with-resources). */
    public Connection getConnection() throws DataAccessException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new DataAccessException(
                    "SQLite JDBC driver not found. Make sure lib/sqlite-jdbc-*.jar is on the classpath.", e);
        }
        try {
            Connection c = DriverManager.getConnection(url);
            try (Statement st = c.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }
            return c;
        } catch (SQLException e) {
            throw new DataAccessException("Cannot open database at " + url + ": " + e.getMessage(), e);
        }
    }

    /** Current schema version, stored in SQLite's PRAGMA user_version. */
    public static final int SCHEMA_VERSION = 2;

    /**
     * Creates all tables if they do not exist yet. Safe to call on every start-up.
     * A version-1 database (single-level "topics") is upgraded: user accounts are kept,
     * the old question bank and results are replaced by the new subject/subtopic structure.
     */
    public void createSchema() throws DataAccessException {
        String[] ddl = {
            "CREATE TABLE IF NOT EXISTS users ("
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " username TEXT NOT NULL UNIQUE COLLATE NOCASE,"
                + " full_name TEXT NOT NULL,"
                + " password_hash TEXT NOT NULL,"
                + " salt TEXT NOT NULL,"
                + " role TEXT NOT NULL CHECK (role IN ('STUDENT','ADMIN')),"
                + " created_at TEXT NOT NULL DEFAULT (datetime('now','localtime')))",
            "CREATE TABLE IF NOT EXISTS subjects ("
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " name TEXT NOT NULL UNIQUE COLLATE NOCASE,"
                + " short_code TEXT NOT NULL,"
                + " description TEXT,"
                + " color TEXT NOT NULL DEFAULT '#2563EB',"
                + " sort_order INTEGER NOT NULL DEFAULT 0)",
            "CREATE TABLE IF NOT EXISTS subtopics ("
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " subject_id INTEGER NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,"
                + " name TEXT NOT NULL COLLATE NOCASE,"
                + " sort_order INTEGER NOT NULL DEFAULT 0,"
                + " UNIQUE (subject_id, name))",
            "CREATE TABLE IF NOT EXISTS questions ("
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " subtopic_id INTEGER NOT NULL REFERENCES subtopics(id) ON DELETE CASCADE,"
                + " difficulty TEXT NOT NULL CHECK (difficulty IN ('EASY','MEDIUM','HARD')),"
                + " question_text TEXT NOT NULL,"
                + " code TEXT, code_lang TEXT,"
                + " option_a TEXT NOT NULL, option_b TEXT NOT NULL,"
                + " option_c TEXT NOT NULL, option_d TEXT NOT NULL,"
                + " correct_index INTEGER NOT NULL CHECK (correct_index BETWEEN 0 AND 3),"
                + " hint TEXT, explanation TEXT)",
            "CREATE TABLE IF NOT EXISTS results ("
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,"
                + " subject_id INTEGER REFERENCES subjects(id) ON DELETE SET NULL,"
                + " subject_name TEXT NOT NULL,"
                + " subtopic_id INTEGER REFERENCES subtopics(id) ON DELETE SET NULL,"
                + " subtopic_name TEXT NOT NULL,"
                + " difficulty TEXT NOT NULL,"
                + " total_questions INTEGER NOT NULL,"
                + " correct INTEGER NOT NULL, wrong INTEGER NOT NULL, unanswered INTEGER NOT NULL,"
                + " hints_used INTEGER NOT NULL DEFAULT 0,"
                + " score REAL NOT NULL, max_score REAL NOT NULL,"
                + " time_limit_sec INTEGER NOT NULL, time_taken_sec INTEGER NOT NULL,"
                + " auto_submitted INTEGER NOT NULL DEFAULT 0,"
                + " taken_at TEXT NOT NULL)",
            // Each answer keeps a snapshot of its question so the review still works after edits/deletes.
            "CREATE TABLE IF NOT EXISTS result_answers ("
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " result_id INTEGER NOT NULL REFERENCES results(id) ON DELETE CASCADE,"
                + " question_id INTEGER,"
                + " subtopic_name TEXT,"
                + " difficulty TEXT NOT NULL,"
                + " question_text TEXT NOT NULL,"
                + " code TEXT, code_lang TEXT,"
                + " option_a TEXT NOT NULL, option_b TEXT NOT NULL,"
                + " option_c TEXT NOT NULL, option_d TEXT NOT NULL,"
                + " correct_index INTEGER NOT NULL,"
                + " selected_index INTEGER NOT NULL,"
                + " hint TEXT, hint_used INTEGER NOT NULL DEFAULT 0,"
                + " explanation TEXT)",
            "CREATE INDEX IF NOT EXISTS idx_subtopics_subject ON subtopics(subject_id)",
            "CREATE INDEX IF NOT EXISTS idx_questions_subtopic ON questions(subtopic_id, difficulty)",
            "CREATE INDEX IF NOT EXISTS idx_results_user ON results(user_id)",
            "CREATE INDEX IF NOT EXISTS idx_answers_result ON result_answers(result_id)"
        };
        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            int version;
            try (java.sql.ResultSet rs = st.executeQuery("PRAGMA user_version")) {
                version = rs.next() ? rs.getInt(1) : 0;
            }
            if (version < SCHEMA_VERSION) {
                boolean legacy;
                try (java.sql.ResultSet rs = st.executeQuery(
                        "SELECT 1 FROM sqlite_master WHERE type='table' AND name='topics'")) {
                    legacy = rs.next();
                }
                if (legacy) {
                    // v1 -> v2: keep users, rebuild everything else
                    st.execute("PRAGMA foreign_keys = OFF");
                    for (String t : new String[]{"result_answers", "results", "questions", "topics"}) {
                        st.execute("DROP TABLE IF EXISTS " + t);
                    }
                    st.execute("PRAGMA foreign_keys = ON");
                    System.out.println("Upgraded database from version 1: accounts kept, question bank rebuilt.");
                }
            }
            for (String sql : ddl) {
                st.execute(sql);
            }
            st.execute("PRAGMA user_version = " + SCHEMA_VERSION);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to create database schema: " + e.getMessage(), e);
        }
    }

    public String getUrl() {
        return url;
    }
}
