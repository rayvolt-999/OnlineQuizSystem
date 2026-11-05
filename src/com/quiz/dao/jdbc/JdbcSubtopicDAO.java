package com.quiz.dao.jdbc;

import com.quiz.dao.SubtopicDAO;
import com.quiz.db.DatabaseManager;
import com.quiz.exception.DataAccessException;
import com.quiz.model.Difficulty;
import com.quiz.model.Subtopic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC (SQLite) implementation of {@link SubtopicDAO}. */
public class JdbcSubtopicDAO implements SubtopicDAO {

    private static final String SELECT =
            "SELECT t.id, t.subject_id, t.name, t.sort_order,"
            + " SUM(CASE WHEN q.difficulty='EASY' THEN 1 ELSE 0 END) AS easy,"
            + " SUM(CASE WHEN q.difficulty='MEDIUM' THEN 1 ELSE 0 END) AS medium,"
            + " SUM(CASE WHEN q.difficulty='HARD' THEN 1 ELSE 0 END) AS hard"
            + " FROM subtopics t LEFT JOIN questions q ON q.subtopic_id = t.id";

    private final DatabaseManager db;

    public JdbcSubtopicDAO(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public List<Subtopic> findBySubject(int subjectId) throws DataAccessException {
        List<Subtopic> list = new ArrayList<>();
        String sql = SELECT + " WHERE t.subject_id = ? GROUP BY t.id ORDER BY t.sort_order, t.id";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, subjectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load subtopics: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public Optional<Subtopic> findById(int id) throws DataAccessException {
        return findOne(SELECT + " WHERE t.id = ? GROUP BY t.id", ps -> ps.setInt(1, id));
    }

    @Override
    public Optional<Subtopic> findByName(int subjectId, String name) throws DataAccessException {
        return findOne(SELECT + " WHERE t.subject_id = ? AND t.name = ? COLLATE NOCASE GROUP BY t.id", ps -> {
            ps.setInt(1, subjectId);
            ps.setString(2, name);
        });
    }

    private interface Binder { void bind(PreparedStatement ps) throws SQLException; }

    private Optional<Subtopic> findOne(String sql, Binder binder) throws DataAccessException {
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load subtopic: " + e.getMessage(), e);
        }
    }

    @Override
    public Subtopic save(Subtopic t) throws DataAccessException {
        try (Connection c = db.getConnection()) {
            if (t.getId() == 0) {
                String sql = "INSERT INTO subtopics(subject_id, name, sort_order) VALUES (?, ?,"
                        + " COALESCE((SELECT MAX(sort_order) + 1 FROM subtopics WHERE subject_id = ?), 1))";
                try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, t.getSubjectId());
                    ps.setString(2, t.getName());
                    ps.setInt(3, t.getSubjectId());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) t.setId(keys.getInt(1));
                    }
                }
            } else {
                try (PreparedStatement ps = c.prepareStatement("UPDATE subtopics SET name=? WHERE id=?")) {
                    ps.setString(1, t.getName());
                    ps.setInt(2, t.getId());
                    if (ps.executeUpdate() == 0) throw new DataAccessException("Subtopic no longer exists");
                }
            }
            return t;
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                throw new DataAccessException("Subtopic '" + t.getName() + "' already exists in this subject", e);
            }
            throw new DataAccessException("Could not save subtopic: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(int id) throws DataAccessException {
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM subtopics WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete subtopic: " + e.getMessage(), e);
        }
    }

    private static Subtopic map(ResultSet rs) throws SQLException {
        Subtopic t = new Subtopic(rs.getInt("id"), rs.getInt("subject_id"), rs.getString("name"));
        t.setSortOrder(rs.getInt("sort_order"));
        t.setQuestionCount(Difficulty.EASY, rs.getInt("easy"));
        t.setQuestionCount(Difficulty.MEDIUM, rs.getInt("medium"));
        t.setQuestionCount(Difficulty.HARD, rs.getInt("hard"));
        return t;
    }
}
