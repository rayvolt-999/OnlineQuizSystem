package com.quiz.dao;

import com.quiz.dao.jdbc.JdbcQuestionDAO;
import com.quiz.dao.jdbc.JdbcResultDAO;
import com.quiz.dao.jdbc.JdbcSubjectDAO;
import com.quiz.dao.jdbc.JdbcSubtopicDAO;
import com.quiz.dao.jdbc.JdbcUserDAO;
import com.quiz.db.DatabaseManager;

/**
 * Single place that decides which DAO implementations are used.
 * The UI and services only see the interfaces, so swapping SQLite for MySQL
 * means changing only this class.
 */
public final class DAOFactory {

    private final SubjectDAO subjectDAO;
    private final SubtopicDAO subtopicDAO;
    private final QuestionDAO questionDAO;
    private final UserDAO userDAO;
    private final ResultDAO resultDAO;

    public DAOFactory(DatabaseManager db) {
        this.subjectDAO = new JdbcSubjectDAO(db);
        this.subtopicDAO = new JdbcSubtopicDAO(db);
        this.questionDAO = new JdbcQuestionDAO(db);
        this.userDAO = new JdbcUserDAO(db);
        this.resultDAO = new JdbcResultDAO(db);
    }

    public SubjectDAO subjects() { return subjectDAO; }
    public SubtopicDAO subtopics() { return subtopicDAO; }
    public QuestionDAO questions() { return questionDAO; }
    public UserDAO users() { return userDAO; }
    public ResultDAO results() { return resultDAO; }
}
