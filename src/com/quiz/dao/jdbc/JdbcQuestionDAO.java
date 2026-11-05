package com.quiz.dao.jdbc;

import com.quiz.dao.QuestionDAO;
import com.quiz.db.DatabaseManager;
import com.quiz.exception.DataAccessException;
import com.quiz.model.Difficulty;
import com.quiz.model.Question;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC (SQLite) implementation of {@link QuestionDAO}. */
public class JdbcQuestionDAO implements QuestionDAO {

    private static final String SELECT =
            "SELECT q.*, t.name AS subtopic_name, s.name AS subject_name, s.id AS subject_id"
            + " FROM questions q JOIN subtopics t ON t.id = q.subtopic_id JOIN subjects s ON s.id = t.subject_id";
    private static final String INSERT = "INSERT INTO questions(subtopic_id, difficulty, question_text, code, code_lang,"
            + " option_a, option_b, option_c, option_d, correct_index, hint, explanation) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
    private static final String UPDATE = "UPDATE questions SET subtopic_id=?, difficulty=?, question_text=?, code=?,"
            + " code_lang=?, option_a=?, option_b=?, option_c=?, option_d=?, correct_index=?, hint=?, explanation=?"
            + " WHERE id=?";

    private final DatabaseManager db;

    public JdbcQuestionDAO(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public List<Question> find(int subjectId, int subtopicId, Difficulty difficulty) throws DataAccessException {
        StringBuilder sql = new StringBuilder(SELECT).append(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (subjectId > 0) { sql.append(" AND s.id = ?"); params.add(subjectId); }
        if (subtopicId > 0) { sql.append(" AND t.id = ?"); params.add(subtopicId); }
        if (difficulty != null) { sql.append(" AND q.difficulty = ?"); params.add(difficulty.name()); }
        sql.append(" ORDER BY s.sort_order, t.sort_order, CASE q.difficulty WHEN 'EASY' THEN 0 WHEN 'MEDIUM' THEN 1 ELSE 2 END, q.id");
        List<Question> list = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load questions: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public Optional<Question> findById(int id) throws DataAccessException {
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(SELECT + " WHERE q.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load question: " + e.getMessage(), e);
        }
    }

    @Override
    public int count() throws DataAccessException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM questions");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not count questions: " + e.getMessage(), e);
        }
    }

    @Override
    public Question save(Question q) throws DataAccessException {
        try (Connection c = db.getConnection()) {
            if (q.getId() == 0) {
                insert(c, q);
            } else {
                try (PreparedStatement ps = c.prepareStatement(UPDATE)) {
                    bind(ps, q);
                    ps.setInt(13, q.getId());
                    if (ps.executeUpdate() == 0) throw new DataAccessException("Question " + q.getId() + " no longer exists");
                }
            }
            return q;
        } catch (SQLException e) {
            throw new DataAccessException("Could not save question: " + e.getMessage(), e);
        }
    }

    @Override
    public int saveAll(List<Question> questions) throws DataAccessException {
        try (Connection c = db.getConnection()) {
            c.setAutoCommit(false);
            try {
                for (Question q : questions) insert(c, q);
                c.commit();
                return questions.size();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Bulk insert failed, nothing was saved: " + e.getMessage(), e);
        }
    }

    private static void insert(Connection c, Question q) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, q);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) q.setId(keys.getInt(1));
            }
        }
    }

    @Override
    public void delete(int id) throws DataAccessException {
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM questions WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete question: " + e.getMessage(), e);
        }
    }

    private static void bind(PreparedStatement ps, Question q) throws SQLException {
        ps.setInt(1, q.getSubtopicId());
        ps.setString(2, q.getDifficulty().name());
        ps.setString(3, q.getText());
        ps.setString(4, q.getCode());
        ps.setString(5, q.getCodeLang());
        for (int i = 0; i < Question.OPTION_COUNT; i++) ps.setString(6 + i, q.getOption(i));
        ps.setInt(10, q.getCorrectIndex());
        ps.setString(11, q.getHint());
        ps.setString(12, q.getExplanation());
    }

    private static Question map(ResultSet rs) throws SQLException {
        String[] opts = {rs.getString("option_a"), rs.getString("option_b"), rs.getString("option_c"), rs.getString("option_d")};
        Question q = new Question(rs.getInt("id"), rs.getInt("subtopic_id"), Difficulty.parse(rs.getString("difficulty")),
                rs.getString("question_text"), opts, rs.getInt("correct_index"));
        q.setCode(rs.getString("code"));
        q.setCodeLang(rs.getString("code_lang"));
        q.setHint(rs.getString("hint"));
        q.setExplanation(rs.getString("explanation"));
        q.setSubtopicName(rs.getString("subtopic_name"));
        q.setSubjectName(rs.getString("subject_name"));
        return q;
    }
}
