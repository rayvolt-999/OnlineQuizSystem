package com.quiz.db;

import com.quiz.dao.DAOFactory;
import com.quiz.exception.DataAccessException;
import com.quiz.model.User;
import com.quiz.service.AdminService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Fills an empty database with default accounts and the bundled question bank
 * (resources/seed/*.qb, listed in resources/seed/index.txt).
 */
public final class DataSeeder {

    public static final String DEFAULT_ADMIN_USER = "admin";
    public static final String DEFAULT_ADMIN_PASS = "admin123";
    public static final String DEMO_STUDENT_USER = "student";
    public static final String DEMO_STUDENT_PASS = "student123";

    private DataSeeder() {
    }

    /** @return problems found in the bundled files (empty when everything loaded cleanly) */
    public static List<String> seedIfEmpty(DAOFactory dao) throws DataAccessException {
        List<String> problems = new ArrayList<>();
        if (dao.users().count() == 0) {
            dao.users().create(DEFAULT_ADMIN_USER, "Administrator", DEFAULT_ADMIN_PASS, User.Role.ADMIN);
            dao.users().create(DEMO_STUDENT_USER, "Demo Student", DEMO_STUDENT_PASS, User.Role.STUDENT);
        }
        if (dao.subjects().findAll().isEmpty()) {
            AdminService admin = new AdminService(dao);
            for (String file : readIndex()) {
                InputStream in = DataSeeder.class.getResourceAsStream("/seed/" + file);
                if (in == null) {
                    problems.add("Missing seed file " + file);
                    continue;
                }
                try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    problems.addAll(admin.importBank(r, file).errors);
                } catch (IOException e) {
                    throw new DataAccessException("Could not read seed file " + file, e);
                }
            }
            if (!problems.isEmpty()) System.err.println("Seed data warnings: " + problems);
        }
        return problems;
    }

    private static List<String> readIndex() throws DataAccessException {
        InputStream in = DataSeeder.class.getResourceAsStream("/seed/index.txt");
        if (in == null) throw new DataAccessException("Seed index /seed/index.txt missing from the classpath");
        List<String> files = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) files.add(line);
            }
        } catch (IOException e) {
            throw new DataAccessException("Could not read seed index", e);
        }
        return files;
    }
}
