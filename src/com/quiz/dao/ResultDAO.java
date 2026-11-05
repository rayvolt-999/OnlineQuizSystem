package com.quiz.dao;

import com.quiz.exception.DataAccessException;
import com.quiz.model.QuizResult;
import com.quiz.model.TopicStat;

import java.util.List;

/** Persistence contract for test attempts, their per-question answers and analytics queries. */
public interface ResultDAO {

    /** Saves the result and all its answers atomically. Sets the generated id on the result. */
    QuizResult save(QuizResult result) throws DataAccessException;

    /** Results of one user, newest first (answers not loaded). */
    List<QuizResult> findByUser(int userId) throws DataAccessException;

    /** Results of everyone, newest first (answers not loaded). */
    List<QuizResult> findAll() throws DataAccessException;

    /** Loads the per-question answers into an already-fetched result (for answer review). */
    void loadAnswers(QuizResult result) throws DataAccessException;

    /** Accuracy per subtopic computed from individual answers; userId 0 = all users. */
    List<TopicStat> subtopicStats(int userId) throws DataAccessException;
}
