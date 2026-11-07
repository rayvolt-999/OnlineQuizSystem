package com.quiz.model;

import com.quiz.util.AppConfig;

/**
 * The three test modes. Each mode fixes how many questions are drawn, the time limit,
 * the marks per question and whether hints are offered.
 * Defaults can be overridden in config.properties (e.g. easy.questions=25).
 */
public enum Difficulty {

    EASY("Easy", 20, 10, 1, false,
            "Core theory and definitions", "Checks your fundamentals"),
    MEDIUM("Medium", 10, 10, 2, true,
            "Theory in depth + predict the output of code", "Tests observation and analytical thinking"),
    HARD("Hard", 5, 10, 3, true,
            "In-depth concepts and tricky code", "Tests mastery and edge cases");

    private final String label;
    private final int defaultQuestions;
    private final int defaultMinutes;
    private final double defaultMarks;
    private final boolean hintsAllowed;
    private final String focus;
    private final String purpose;

    Difficulty(String label, int questions, int minutes, double marks, boolean hints, String focus, String purpose) {
        this.label = label;
        this.defaultQuestions = questions;
        this.defaultMinutes = minutes;
        this.defaultMarks = marks;
        this.hintsAllowed = hints;
        this.focus = focus;
        this.purpose = purpose;
    }

    public String getLabel() { return label; }
    public String getFocus() { return focus; }
    public String getPurpose() { return purpose; }
    public boolean isHintsAllowed() { return hintsAllowed; }

    private String key() { return name().toLowerCase(); }

    /** Questions drawn per test. */
    public int getQuestionCount() {
        return Math.max(1, (int) AppConfig.getDouble(key() + ".questions", defaultQuestions));   // a test needs >= 1 question
    }

    /** Time limit in minutes. */
    public int getMinutes() {
        return Math.max(0, (int) AppConfig.getDouble(key() + ".minutes", defaultMinutes));      // 0 = no time limit
    }

    /** Marks for each correct answer. */
    public double getMarks() {
        return AppConfig.getDouble(key() + ".marks", defaultMarks);
    }

    public static Difficulty parse(String s) {
        if (s == null || s.trim().isEmpty()) return MEDIUM;
        try {
            return valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return MEDIUM;
        }
    }

    @Override
    public String toString() {
        return label;
    }
}
