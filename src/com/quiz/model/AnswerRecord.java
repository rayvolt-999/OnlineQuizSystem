package com.quiz.model;

/**
 * What a student did on one question of a finished attempt.
 * selectedIndex == -1 means unanswered.
 */
public class AnswerRecord {

    public static final int UNANSWERED = -1;

    private final Question question;
    private final int selectedIndex;
    private final boolean hintUsed;

    public AnswerRecord(Question question, int selectedIndex, boolean hintUsed) {
        this.question = question;
        this.selectedIndex = selectedIndex;
        this.hintUsed = hintUsed;
    }

    public Question getQuestion() { return question; }
    public int getSelectedIndex() { return selectedIndex; }
    public boolean isHintUsed() { return hintUsed; }

    public boolean isAnswered() { return selectedIndex != UNANSWERED; }

    public boolean isCorrect() { return isAnswered() && question.isCorrect(selectedIndex); }

    public Status getStatus() {
        if (!isAnswered()) return Status.UNANSWERED;
        return isCorrect() ? Status.CORRECT : Status.WRONG;
    }

    public enum Status { CORRECT, WRONG, UNANSWERED }
}
