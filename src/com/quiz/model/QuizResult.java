package com.quiz.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Outcome of one test attempt, including per-question answers for the review screen. */
public class QuizResult {

    public static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    public static final String ALL_SUBTOPICS = "All subtopics";

    private int id;
    private int userId;
    private String userName;
    private int subjectId;
    private String subjectName;
    private int subtopicId;          // 0 = all subtopics (mixed)
    private String subtopicName;     // ALL_SUBTOPICS when mixed
    private Difficulty difficulty;
    private int totalQuestions;
    private int correct;
    private int wrong;
    private int unanswered;
    private int hintsUsed;
    private double score;
    private double maxScore;
    private int timeLimitSeconds;
    private int timeTakenSeconds;
    private boolean autoSubmitted;
    private LocalDateTime takenAt;
    private final List<AnswerRecord> answers = new ArrayList<>();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public int getSubjectId() { return subjectId; }
    public void setSubjectId(int subjectId) { this.subjectId = subjectId; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public int getSubtopicId() { return subtopicId; }
    public void setSubtopicId(int subtopicId) { this.subtopicId = subtopicId; }

    public String getSubtopicName() { return subtopicName; }
    public void setSubtopicName(String subtopicName) { this.subtopicName = subtopicName; }

    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }

    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }

    public int getCorrect() { return correct; }
    public void setCorrect(int correct) { this.correct = correct; }

    public int getWrong() { return wrong; }
    public void setWrong(int wrong) { this.wrong = wrong; }

    public int getUnanswered() { return unanswered; }
    public void setUnanswered(int unanswered) { this.unanswered = unanswered; }

    public int getHintsUsed() { return hintsUsed; }
    public void setHintsUsed(int hintsUsed) { this.hintsUsed = hintsUsed; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public double getMaxScore() { return maxScore; }
    public void setMaxScore(double maxScore) { this.maxScore = maxScore; }

    public int getTimeLimitSeconds() { return timeLimitSeconds; }
    public void setTimeLimitSeconds(int timeLimitSeconds) { this.timeLimitSeconds = timeLimitSeconds; }

    public int getTimeTakenSeconds() { return timeTakenSeconds; }
    public void setTimeTakenSeconds(int timeTakenSeconds) { this.timeTakenSeconds = timeTakenSeconds; }

    public boolean isAutoSubmitted() { return autoSubmitted; }
    public void setAutoSubmitted(boolean autoSubmitted) { this.autoSubmitted = autoSubmitted; }

    public LocalDateTime getTakenAt() { return takenAt; }
    public void setTakenAt(LocalDateTime takenAt) { this.takenAt = takenAt; }

    public List<AnswerRecord> getAnswers() { return Collections.unmodifiableList(answers); }
    public void addAnswer(AnswerRecord record) { answers.add(record); }

    /** "Data Structures › Linked Lists" or "Data Structures" when mixed. */
    public String getTitle() {
        return subtopicId == 0 || subtopicName == null || ALL_SUBTOPICS.equals(subtopicName)
                ? subjectName : subjectName + " › " + subtopicName;
    }

    public double getPercentage() {
        return maxScore <= 0 ? 0 : Math.max(0, score) * 100.0 / maxScore;
    }

    public double getAccuracy() {
        int attempted = correct + wrong;
        return attempted == 0 ? 0 : correct * 100.0 / attempted;
    }

    public String getGrade() {
        double p = getPercentage();
        if (p >= 90) return "A+";
        if (p >= 80) return "A";
        if (p >= 70) return "B";
        if (p >= 60) return "C";
        if (p >= 40) return "D";
        return "F";
    }

    public boolean isPassed() {
        return getPercentage() >= 40;
    }

    public static String formatDuration(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }
}
