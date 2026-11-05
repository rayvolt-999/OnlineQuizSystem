package com.quiz.model;

import java.util.EnumMap;
import java.util.Map;

/** A sub-category inside a subject, e.g. "Linked Lists" inside Data Structures. */
public class Subtopic {

    private int id;
    private int subjectId;
    private String name;
    private int sortOrder;
    private final Map<Difficulty, Integer> questionCounts = new EnumMap<>(Difficulty.class);

    public Subtopic() {
    }

    public Subtopic(int id, int subjectId, String name) {
        this.id = id;
        this.subjectId = subjectId;
        this.name = name;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSubjectId() { return subjectId; }
    public void setSubjectId(int subjectId) { this.subjectId = subjectId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public int getQuestionCount(Difficulty d) { return questionCounts.getOrDefault(d, 0); }
    public void setQuestionCount(Difficulty d, int n) { questionCounts.put(d, n); }

    public int getTotalQuestions() {
        return questionCounts.values().stream().mapToInt(Integer::intValue).sum();
    }

    @Override
    public String toString() {
        return name;
    }
}
