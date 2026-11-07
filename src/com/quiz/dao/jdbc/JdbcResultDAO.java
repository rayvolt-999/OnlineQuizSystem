package com.quiz.dao.jdbc;

import com.quiz.dao.ResultDAO;
import com.quiz.db.DatabaseManager;
import com.quiz.exception.DataAccessException;
import com.quiz.model.AnswerRecord;
import com.quiz.model.Difficulty;
import com.quiz.model.Question;
import com.quiz.model.QuizResult;
import com.quiz.model.TopicStat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** JDBC (SQLite) implementation of {@link ResultDAO}. A result is saved in one transaction. */
public class JdbcResultDAO implements ResultDAO {

    private static final String SELECT = "SELECT r.*, u.full_name FROM results r JOIN users u ON u.id = r.user_id";

    private final DatabaseManager db;

    public JdbcResultDAO(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public QuizResult save(QuizResult r) throws DataAccessException {
        String insResult = "INSERT INTO results(user_id, subject_id, subject_name, subtopic_id, subtopic_name, difficulty,"
                + " total_questions, correct, wrong, unanswered, hints_used, score, max_score, time_limit_sec,"
                + " time_taken_sec, auto_submitted, taken_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        String insAnswer = "INSERT INTO result_answers(result_id, question_id, subtopic_name, difficulty, question_text,"
                + " code, code_lang, option_a, option_b, option_c, option_d, correct_index, selected_index, hint,"
                + " hint_used, explanation) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = db.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(insResult, Statement.RETURN_GENERATED_KEYS)) {
                    int i = 1;
                    ps.setInt(i++, r.getUserId());
                    setNullableId(ps, i++, r.getSubjectId());
                    ps.setString(i++, r.getSubjectName());
                    setNullableId(ps, i++, r.getSubtopicId());
                    ps.setString(i++, r.getSubtopicName());
                    ps.setString(i++, r.getDifficulty().name());
                    ps.setInt(i++, r.getTotalQuestions());
                    ps.setInt(i++, r.getCorrect());
                    ps.setInt(i++, r.getWrong());
                    ps.setInt(i++, r.getUnanswered());
                    ps.setInt(i++, r.getHintsUsed());
                    ps.setDouble(i++, r.getScore());
                    ps.setDouble(i++, r.getMaxScore());
                    ps.setInt(i++, r.getTimeLimitSeconds());
                    ps.setInt(i++, r.getTimeTakenSeconds());
                    ps.setInt(i++, r.isAutoSubmitted() ? 1 : 0);
                    ps.setString(i, r.getTakenAt().toString());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) r.setId(keys.getInt(1));
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(insAnswer)) {
                    for (AnswerRecord a : r.getAnswers()) {
                        Question q = a.getQuestion();
                        int i = 1;
                        ps.setInt(i++, r.getId());
                        ps.setInt(i++, q.getId());
                        ps.setString(i++, q.getSubtopicName());
                        ps.setString(i++, q.getDifficulty().name());
                        ps.setString(i++, q.getText());
                        ps.setString(i++, q.getCode());
                        ps.setString(i++, q.getCodeLang());
                        for (int k = 0; k < Question.OPTION_COUNT; k++) ps.setString(i++, q.getOption(k));
                        ps.setInt(i++, q.getCorrectIndex());
                        ps.setInt(i++, a.getSelectedIndex());
                        ps.setString(i++, q.getHint());
                        ps.setInt(i++, a.isHintUsed() ? 1 : 0);
                        ps.setString(i, q.getExplanation());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                c.commit();
                return r;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not save result: " + e.getMessage(), e);
        }
    }

    private static void setNullableId(PreparedStatement ps, int idx, int id) throws SQLException {
        if (id > 0) ps.setInt(idx, id); else ps.setNull(idx, Types.INTEGER);
    }

    @Override
    public List<QuizResult> findByUser(int userId) throws DataAccessException {
        return query(SELECT + " WHERE r.user_id = ? ORDER BY r.taken_at DESC, r.id DESC", userId);
    }

    @Override
    public List<QuizResult> findAll() throws DataAccessException {
        return query(SELECT + " ORDER BY r.taken_at DESC, r.id DESC", null);
    }

    private List<QuizResult> query(String sql, Integer param) throws DataAccessException {
        List<QuizResult> list = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (param != null) ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    QuizResult r = new QuizResult();
                    r.setId(rs.getInt("id"));
                    r.setUserId(rs.getInt("user_id"));
                    r.setUserName(rs.getString("full_name"));
                    r.setSubjectId(rs.getInt("subject_id"));
                    r.setSubjectName(rs.getString("subject_name"));
                    r.setSubtopicId(rs.getInt("subtopic_id"));
                    r.setSubtopicName(rs.getString("subtopic_name"));
                    r.setDifficulty(Difficulty.parse(rs.getString("difficulty")));
                    r.setTotalQuestions(rs.getInt("total_questions"));
                    r.setCorrect(rs.getInt("correct"));
                    r.setWrong(rs.getInt("wrong"));
                    r.setUnanswered(rs.getInt("unanswered"));
                    r.setHintsUsed(rs.getInt("hints_used"));
                    r.setScore(rs.getDouble("score"));
                    r.setMaxScore(rs.getDouble("max_score"));
                    r.setTimeLimitSeconds(rs.getInt("time_limit_sec"));
                    r.setTimeTakenSeconds(rs.getInt("time_taken_sec"));
                    r.setAutoSubmitted(rs.getInt("auto_submitted") == 1);
                    r.setTakenAt(LocalDateTime.parse(rs.getString("taken_at")));
                    list.add(r);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load results: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public void loadAnswers(QuizResult r) throws DataAccessException {
        if (!r.getAnswers().isEmpty()) return;
        String sql = "SELECT * FROM result_answers WHERE result_id = ? ORDER BY id";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, r.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String[] opts = {rs.getString("option_a"), rs.getString("option_b"),
                        rs.getString("option_c"), rs.getString("option_d")};
                    Question q = new Question(rs.getInt("question_id"), 0, Difficulty.parse(rs.getString("difficulty")),
                            rs.getString("question_text"), opts, rs.getInt("correct_index"));
                    q.setCode(rs.getString("code"));
                    q.setCodeLang(rs.getString("code_lang"));
                    q.setHint(rs.getString("hint"));
                    q.setExplanation(rs.getString("explanation"));
                    q.setSubtopicName(rs.getString("subtopic_name"));
                    q.setSubjectName(r.getSubjectName());
                    r.addAnswer(new AnswerRecord(q, rs.getInt("selected_index"), rs.getInt("hint_used") == 1));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load answers: " + e.getMessage(), e);
        }
    }

    @Override
    public List<TopicStat> subtopicStats(int userId) throws DataAccessException {
        String sql = "SELECT r.subject_name, a.subtopic_name, COUNT(*) AS attempted,"
                + " SUM(CASE WHEN a.selected_index = a.correct_index THEN 1 ELSE 0 END) AS correct"
                + " FROM result_answers a JOIN results r ON r.id = a.result_id"
                // skipped questions are not evidence of a weak topic (and accuracy = correct / attempted elsewhere too)
                + " WHERE a.subtopic_name IS NOT NULL AND a.selected_index >= 0" + (userId > 0 ? " AND r.user_id = ?" : "")
                + " GROUP BY r.subject_name, a.subtopic_name";
        List<TopicStat> list = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (userId > 0) ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new TopicStat(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getInt(4)));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not compute statistics: " + e.getMessage(), e);
        }
        Collections.sort(list);
        return list;
    }
}
