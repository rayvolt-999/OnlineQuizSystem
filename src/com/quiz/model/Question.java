package com.quiz.model;

import java.util.Arrays;
import java.util.Objects;

/**
 * A multiple-choice question with exactly four options.
 * Optionally carries a code snippet (for "predict the output" questions),
 * a hint (offered in Medium/Hard tests) and an explanation (shown in the answer review).
 */
public class Question {

    public static final int OPTION_COUNT = 4;
    public static final char[] LABELS = {'A', 'B', 'C', 'D'};

    private int id;
    private int subtopicId;
    private Difficulty difficulty = Difficulty.EASY;
    private String text;
    private String code;        // may be null
    private String codeLang;    // java, python, c, cpp, bash, sql ... may be null
    private final String[] options = new String[OPTION_COUNT];
    private int correctIndex;
    private String hint;        // may be null
    private String explanation; // may be null

    // display-only, filled by DAO joins
    private String subtopicName;
    private String subjectName;

    public Question() {
    }

    public Question(int id, int subtopicId, Difficulty difficulty, String text, String[] options, int correctIndex) {
        this.id = id;
        this.subtopicId = subtopicId;
        this.difficulty = difficulty == null ? Difficulty.EASY : difficulty;
        this.text = text;
        setOptions(options);
        this.correctIndex = correctIndex;
    }

    /** Deep copy, used so edit dialogs never mutate the original until saved. */
    public Question copy() {
        Question q = new Question(id, subtopicId, difficulty, text, options, correctIndex);
        q.code = code;
        q.codeLang = codeLang;
        q.hint = hint;
        q.explanation = explanation;
        q.subtopicName = subtopicName;
        q.subjectName = subjectName;
        return q;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSubtopicId() { return subtopicId; }
    public void setSubtopicId(int subtopicId) { this.subtopicId = subtopicId; }

    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = blankToNull(code); }

    public boolean hasCode() { return code != null && !code.trim().isEmpty(); }

    public String getCodeLang() { return codeLang; }
    public void setCodeLang(String codeLang) { this.codeLang = blankToNull(codeLang); }

    public String[] getOptions() { return options.clone(); }

    public String getOption(int index) { return options[index]; }

    public final void setOptions(String[] opts) {
        if (opts == null || opts.length != OPTION_COUNT) {
            throw new IllegalArgumentException("A question needs exactly " + OPTION_COUNT + " options");
        }
        System.arraycopy(opts, 0, options, 0, OPTION_COUNT);
    }

    public int getCorrectIndex() { return correctIndex; }
    public void setCorrectIndex(int correctIndex) { this.correctIndex = correctIndex; }

    public String getHint() { return hint; }
    public void setHint(String hint) { this.hint = blankToNull(hint); }

    public boolean hasHint() { return hint != null; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = blankToNull(explanation); }

    public String getSubtopicName() { return subtopicName; }
    public void setSubtopicName(String subtopicName) { this.subtopicName = subtopicName; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public boolean isCorrect(int selectedIndex) {
        return selectedIndex == correctIndex;
    }

    public static String label(int index) {
        return index >= 0 && index < OPTION_COUNT ? String.valueOf(LABELS[index]) : "-";
    }

    private static String blankToNull(String s) {
        return s == null || s.trim().isEmpty() ? null : s;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Question)) return false;
        Question q = (Question) o;
        return id == q.id && subtopicId == q.subtopicId && correctIndex == q.correctIndex
                && difficulty == q.difficulty && Objects.equals(text, q.text)
                && Objects.equals(code, q.code) && Arrays.equals(options, q.options);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, subtopicId, difficulty, text, code, correctIndex) * 31 + Arrays.hashCode(options);
    }

    @Override
    public String toString() {
        return "Q" + id + " [" + difficulty + "]: " + text;
    }
}
