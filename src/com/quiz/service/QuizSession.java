package com.quiz.service;

import com.quiz.model.AnswerRecord;
import com.quiz.model.Difficulty;
import com.quiz.model.Question;
import com.quiz.model.QuizResult;
import com.quiz.model.Subject;
import com.quiz.model.Subtopic;
import com.quiz.model.User;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * In-memory state of one test attempt.
 * <ul>
 *   <li>{@code questions} – ordered List of the questions drawn for this attempt</li>
 *   <li>{@code answers} – Map from question position to chosen option (absent = unanswered)</li>
 *   <li>{@code marked} – sorted Set of positions flagged for review</li>
 *   <li>{@code hintsShown} – Set of positions whose hint was revealed</li>
 * </ul>
 */
public class QuizSession {

    private final User user;
    private final Subject subject;
    private final Subtopic subtopic;          // null = all subtopics
    private final Difficulty difficulty;
    private final List<Question> questions;
    private final Map<Integer, Integer> answers = new HashMap<>();
    private final Set<Integer> marked = new TreeSet<>();
    private final Set<Integer> hintsShown = new HashSet<>();
    private final long startMillis;
    private final int timeLimitSeconds;
    private int currentIndex;
    private boolean finished;

    public QuizSession(User user, Subject subject, Subtopic subtopic, Difficulty difficulty,
                       List<Question> questions, int timeLimitSeconds) {
        if (questions == null || questions.isEmpty()) {
            throw new IllegalArgumentException("A test needs at least one question");
        }
        this.user = user;
        this.subject = subject;
        this.subtopic = subtopic;
        this.difficulty = difficulty;
        this.questions = Collections.unmodifiableList(new ArrayList<>(questions));
        this.timeLimitSeconds = Math.max(0, timeLimitSeconds);
        this.startMillis = System.currentTimeMillis();
    }

    // ---------- navigation ----------

    public int size() { return questions.size(); }
    public int getCurrentIndex() { return currentIndex; }
    public Question getCurrentQuestion() { return questions.get(currentIndex); }
    public Question getQuestion(int index) { return questions.get(index); }

    public void goTo(int index) {
        if (index < 0 || index >= questions.size()) {
            throw new IndexOutOfBoundsException("No question at position " + (index + 1));
        }
        currentIndex = index;
    }

    public boolean hasNext() { return currentIndex < questions.size() - 1; }
    public boolean hasPrevious() { return currentIndex > 0; }
    public void next() { if (hasNext()) currentIndex++; }
    public void previous() { if (hasPrevious()) currentIndex--; }

    // ---------- answering ----------

    public void selectAnswer(int optionIndex) {
        ensureOpen();
        if (optionIndex < 0 || optionIndex >= Question.OPTION_COUNT) {
            throw new IllegalArgumentException("Invalid option " + optionIndex);
        }
        answers.put(currentIndex, optionIndex);
    }

    public void clearAnswer() {
        ensureOpen();
        answers.remove(currentIndex);
    }

    public int getAnswer(int index) { return answers.getOrDefault(index, AnswerRecord.UNANSWERED); }
    public boolean isAnswered(int index) { return answers.containsKey(index); }
    public int answeredCount() { return answers.size(); }
    public int unansweredCount() { return questions.size() - answers.size(); }

    /** 1-based question numbers that have no answer yet. */
    public List<Integer> unansweredNumbers() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            if (!answers.containsKey(i)) list.add(i + 1);
        }
        return list;
    }

    public void toggleMarked() { if (!marked.remove(currentIndex)) marked.add(currentIndex); }
    public boolean isMarked(int index) { return marked.contains(index); }
    public int markedCount() { return marked.size(); }

    // ---------- hints ----------

    /** Hints exist only in modes that allow them and only if the question has one. */
    public boolean isHintAvailable(int index) {
        return difficulty.isHintsAllowed() && questions.get(index).hasHint();
    }

    /** Reveals the hint for the current question and records it. Returns the hint text. */
    public String revealHint() {
        ensureOpen();
        if (!isHintAvailable(currentIndex)) {
            throw new IllegalStateException("No hint for this question");
        }
        hintsShown.add(currentIndex);
        return getCurrentQuestion().getHint();
    }

    public boolean isHintShown(int index) { return hintsShown.contains(index); }
    public int hintsUsed() { return hintsShown.size(); }

    // ---------- timing ----------

    public int getElapsedSeconds() { return (int) ((System.currentTimeMillis() - startMillis) / 1000); }
    public boolean hasTimeLimit() { return timeLimitSeconds > 0; }
    public int getTimeLimitSeconds() { return timeLimitSeconds; }
    public int getRemainingSeconds() { return Math.max(0, timeLimitSeconds - getElapsedSeconds()); }
    public boolean isTimeUp() { return hasTimeLimit() && getRemainingSeconds() == 0; }

    // ---------- scoring ----------

    /**
     * Closes the session and builds the result.
     * Correct: +marksPerQuestion, wrong: -wrongPenalty, unanswered: 0. Hints are recorded but cost nothing.
     */
    public QuizResult finish(double marksPerQuestion, double wrongPenalty, boolean autoSubmitted) {
        ensureOpen();
        finished = true;
        QuizResult r = new QuizResult();
        r.setUserId(user.getId());
        r.setUserName(user.getFullName());
        r.setSubjectId(subject.getId());
        r.setSubjectName(subject.getName());
        r.setSubtopicId(subtopic == null ? 0 : subtopic.getId());
        r.setSubtopicName(subtopic == null ? QuizResult.ALL_SUBTOPICS : subtopic.getName());
        r.setDifficulty(difficulty);
        r.setTotalQuestions(questions.size());
        int correct = 0;
        int wrong = 0;
        for (int i = 0; i < questions.size(); i++) {
            AnswerRecord rec = new AnswerRecord(questions.get(i), getAnswer(i), hintsShown.contains(i));
            r.addAnswer(rec);
            switch (rec.getStatus()) {
                case CORRECT: correct++; break;
                case WRONG: wrong++; break;
                default: break;
            }
        }
        r.setCorrect(correct);
        r.setWrong(wrong);
        r.setUnanswered(questions.size() - correct - wrong);
        r.setHintsUsed(hintsShown.size());
        r.setScore(correct * marksPerQuestion - wrong * wrongPenalty);
        r.setMaxScore(questions.size() * marksPerQuestion);
        r.setTimeLimitSeconds(timeLimitSeconds);
        int elapsed = getElapsedSeconds();
        r.setTimeTakenSeconds(hasTimeLimit() ? Math.min(elapsed, timeLimitSeconds) : elapsed);
        r.setAutoSubmitted(autoSubmitted);
        r.setTakenAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        return r;
    }

    public boolean isFinished() { return finished; }

    private void ensureOpen() {
        if (finished) throw new IllegalStateException("This test has already been submitted");
    }

    public User getUser() { return user; }
    public Subject getSubject() { return subject; }
    public Subtopic getSubtopic() { return subtopic; }
    public Difficulty getDifficulty() { return difficulty; }

    /** "Data Structures › Linked Lists" or just the subject when mixed. */
    public String getTitle() {
        return subtopic == null ? subject.getName() : subject.getName() + " › " + subtopic.getName();
    }
}
