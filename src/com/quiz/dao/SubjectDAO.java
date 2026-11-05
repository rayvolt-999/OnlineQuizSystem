package com.quiz.dao;

import com.quiz.exception.DataAccessException;
import com.quiz.model.Subject;

import java.util.List;
import java.util.Optional;

/** Persistence contract for subjects. Returned subjects carry subtopic and per-difficulty question counts. */
public interface SubjectDAO {

    List<Subject> findAll() throws DataAccessException;

    Optional<Subject> findById(int id) throws DataAccessException;

    Optional<Subject> findByName(String name) throws DataAccessException;

    /** Inserts when id == 0, otherwise updates. */
    Subject save(Subject subject) throws DataAccessException;

    /** Deletes the subject with all its subtopics and questions (ON DELETE CASCADE). Results are kept. */
    void delete(int id) throws DataAccessException;
}
