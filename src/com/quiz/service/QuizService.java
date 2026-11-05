package com.quiz.service;

import com.quiz.dao.DAOFactory;
import com.quiz.exception.DataAccessException;
import com.quiz.exception.ValidationException;
import com.quiz.model.Difficulty;
import com.quiz.model.Question;
import com.quiz.model.QuizResult;
import com.quiz.model.Subject;
import com.quiz.model.Subtopic;
import com.quiz.model.TopicStat;
import com.quiz.model.User;
import com.quiz.util.AppConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Starts tests, scores and stores them, and serves history and analytics. */
public class QuizService {

    private final DAOFactory dao;
    private final Random random;

    public QuizService(DAOFactory dao) {
        this(dao, new Random());
    }

    /** Allows a seeded Random so tests are repeatable. */
    public QuizService(DAOFactory dao, Random random) {
        this.dao = dao;
        this.random = random;
    }

    public List<Subject> getSubjects() throws DataAccessException {
        return dao.subjects().findAll();
    }

    public Subject getSubject(int id) throws DataAccessException {
        return dao.subjects().findById(id).orElse(null);
    }

    public List<Subtopic> getSubtopics(Subject subject) throws DataAccessException {
        return dao.subtopics().findBySubject(subject.getId());
    }

    /** How many questions a test will actually contain (bank may hold fewer than the mode's count). */
    public static int plannedQuestionCount(int available, Difficulty d) {
        return Math.min(available, d.getQuestionCount());
    }

    /**
     * Draws a shuffled set of questions and opens a new session.
     * @param subtopic null = mix of all subtopics in the subject
     */
    public QuizSession startQuiz(User user, Subject subject, Subtopic subtopic, Difficulty difficulty)
            throws ValidationException, DataAccessException {
        if (user == null) throw new ValidationException("You must be logged in to take a test.");
        if (subject == null) throw new ValidationException("Select a subject first.");
        if (difficulty == null) throw new ValidationException("Select Easy, Medium or Hard.");
        List<Question> pool = new ArrayList<>(dao.questions().find(subject.getId(),
                subtopic == null ? 0 : subtopic.getId(), difficulty));
        if (pool.isEmpty()) {
            throw new ValidationException("There are no " + difficulty.getLabel() + " questions for "
                    + (subtopic == null ? subject.getName() : subtopic.getName()) + " yet.");
        }
        Collections.shuffle(pool, random);
        int n = plannedQuestionCount(pool.size(), difficulty);
        return new QuizSession(user, subject, subtopic, difficulty, pool.subList(0, n), difficulty.getMinutes() * 60);
    }

    /** Scores the session and persists it. */
    public QuizResult submit(QuizSession session, boolean autoSubmitted) throws DataAccessException {
        return save(evaluate(session, autoSubmitted));
    }

    /** Closes and scores the session without saving (so the UI can still show a result if saving fails). */
    public QuizResult evaluate(QuizSession session, boolean autoSubmitted) {
        return session.finish(session.getDifficulty().getMarks(), AppConfig.marksWrong(), autoSubmitted);
    }

    public QuizResult save(QuizResult result) throws DataAccessException {
        return dao.results().save(result);
    }

    public List<QuizResult> historyFor(User user) throws DataAccessException {
        return user.isAdmin() ? dao.results().findAll() : dao.results().findByUser(user.getId());
    }

    public void loadAnswers(QuizResult result) throws DataAccessException {
        dao.results().loadAnswers(result);
    }

    /** Per-subtopic accuracy, weakest first. Admins see the whole class. */
    public List<TopicStat> subtopicStats(User user) throws DataAccessException {
        return dao.results().subtopicStats(user.isAdmin() ? 0 : user.getId());
    }
}
