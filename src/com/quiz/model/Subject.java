package com.quiz.model;

import java.util.EnumMap;
import java.util.Map;

/** An academic subject (e.g. Data Structures). Contains subtopics, which contain questions. */
public class Subject {

    private int id;
    private String name;
    private String shortCode;     // 2-3 letters shown on the subject badge, e.g. "DS"
    private String description;
    private String color;         // accent colour as #RRGGBB
    private int sortOrder;
    private int subtopicCount;    // filled by DAO
    private final Map<Difficulty, Integer> questionCounts = new EnumMap<>(Difficulty.class); // filled by DAO

    public Subject() {
    }

    public Subject(int id, String name, String shortCode, String description, String color) {
        this.id = id;
        this.name = name;
        this.shortCode = shortCode;
        this.description = description;
        this.color = color;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public int getSubtopicCount() { return subtopicCount; }
    public void setSubtopicCount(int subtopicCount) { this.subtopicCount = subtopicCount; }

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
