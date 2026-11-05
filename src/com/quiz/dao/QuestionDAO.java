package com.quiz.dao;

import com.quiz.exception.DataAccessException;
import com.quiz.model.Difficulty;
import com.quiz.model.Question;

import java.util.List;
import java.util.Optional;

/** Persistence contract for MCQs. */
public interface QuestionDAO {

    /**
     * Flexible search used by both the quiz engine and the admin panel.
     * Any argument may be null / 0 to mean "any".
     */
    List<Question> find(int subjectId, int subtopicId, Difficulty difficulty) throws DataAccessException;

    Optional<Question> findById(int id) throws DataAccessException;

    int count() throws DataAccessException;

    /** Inserts when id == 0, otherwise updates. */
    Question save(Question q) throws DataAccessException;

    /** Inserts many questions in a single transaction (all or nothing). */
    int saveAll(List<Question> questions) throws DataAccessException;

    void delete(int id) throws DataAccessException;
}
