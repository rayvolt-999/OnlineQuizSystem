package com.quiz.dao.jdbc;

import com.quiz.dao.SubjectDAO;
import com.quiz.db.DatabaseManager;
import com.quiz.exception.DataAccessException;
import com.quiz.model.Difficulty;
import com.quiz.model.Subject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC (SQLite) implementation of {@link SubjectDAO}. */
public class JdbcSubjectDAO implements SubjectDAO {

    private static final String SELECT =
            "SELECT s.id, s.name, s.short_code, s.description, s.color, s.sort_order,"
            + " (SELECT COUNT(*) FROM subtopics t WHERE t.subject_id = s.id) AS subtopics,"
            + " (SELECT COUNT(*) FROM questions q JOIN subtopics t ON t.id = q.subtopic_id"
            + "    WHERE t.subject_id = s.id AND q.difficulty = 'EASY') AS easy,"
            + " (SELECT COUNT(*) FROM questions q JOIN subtopics t ON t.id = q.subtopic_id"
            + "    WHERE t.subject_id = s.id AND q.difficulty = 'MEDIUM') AS medium,"
            + " (SELECT COUNT(*) FROM questions q JOIN subtopics t ON t.id = q.subtopic_id"
            + "    WHERE t.subject_id = s.id AND q.difficulty = 'HARD') AS hard"
            + " FROM subjects s";

    private final DatabaseManager db;

    public JdbcSubjectDAO(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public List<Subject> findAll() throws DataAccessException {
        List<Subject> list = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + " ORDER BY s.sort_order, s.name COLLATE NOCASE");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new DataAccessException("Could not load subjects: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public Optional<Subject> findById(int id) throws DataAccessException {
        return findOne(SELECT + " WHERE s.id = ?", id);
    }

    @Override
    public Optional<Subject> findByName(String name) throws DataAccessException {
        return findOne(SELECT + " WHERE s.name = ? COLLATE NOCASE", name);
    }

    private Optional<Subject> findOne(String sql, Object param) throws DataAccessException {
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load subject: " + e.getMessage(), e);
        }
    }

    @Override
    public Subject save(Subject s) throws DataAccessException {
        try (Connection c = db.getConnection()) {
            if (s.getId() == 0) {
                String sql = "INSERT INTO subjects(name, short_code, description, color, sort_order) VALUES (?,?,?,?,"
                        + " COALESCE((SELECT MAX(sort_order) + 1 FROM subjects), 1))";
                try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    bind(ps, s);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) s.setId(keys.getInt(1));
                    }
                }
            } else {
                String sql = "UPDATE subjects SET name=?, short_code=?, description=?, color=? WHERE id=?";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    bind(ps, s);
                    ps.setInt(5, s.getId());
                    if (ps.executeUpdate() == 0) throw new DataAccessException("Subject no longer exists");
                }
            }
            return s;
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                throw new DataAccessException("A subject named '" + s.getName() + "' already exists", e);
            }
            throw new DataAccessException("Could not save subject: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(int id) throws DataAccessException {
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM subjects WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete subject: " + e.getMessage(), e);
        }
    }

    private static void bind(PreparedStatement ps, Subject s) throws SQLException {
        ps.setString(1, s.getName());
        ps.setString(2, s.getShortCode());
        ps.setString(3, s.getDescription());
        ps.setString(4, s.getColor() == null ? "#2563EB" : s.getColor());
    }

    private static Subject map(ResultSet rs) throws SQLException {
        Subject s = new Subject(rs.getInt("id"), rs.getString("name"), rs.getString("short_code"),
                rs.getString("description"), rs.getString("color"));
        s.setSortOrder(rs.getInt("sort_order"));
        s.setSubtopicCount(rs.getInt("subtopics"));
        s.setQuestionCount(Difficulty.EASY, rs.getInt("easy"));
        s.setQuestionCount(Difficulty.MEDIUM, rs.getInt("medium"));
        s.setQuestionCount(Difficulty.HARD, rs.getInt("hard"));
        return s;
    }
}
