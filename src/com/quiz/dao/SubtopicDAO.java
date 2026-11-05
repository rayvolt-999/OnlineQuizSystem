package com.quiz.dao;

import com.quiz.exception.DataAccessException;
import com.quiz.model.Subtopic;

import java.util.List;
import java.util.Optional;

/** Persistence contract for subtopics (sub-categories of a subject). */
public interface SubtopicDAO {

    /** Subtopics of a subject in display order, with per-difficulty question counts. */
    List<Subtopic> findBySubject(int subjectId) throws DataAccessException;

    Optional<Subtopic> findById(int id) throws DataAccessException;

    Optional<Subtopic> findByName(int subjectId, String name) throws DataAccessException;

    Subtopic save(Subtopic subtopic) throws DataAccessException;

    void delete(int id) throws DataAccessException;
}
