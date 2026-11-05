package com.quiz.model;

/** Aggregated accuracy for one subtopic, used by the analytics "focus areas" list. */
public class TopicStat implements Comparable<TopicStat> {

    private final String subjectName;
    private final String subtopicName;
    private final int attempted;
    private final int correct;

    public TopicStat(String subjectName, String subtopicName, int attempted, int correct) {
        this.subjectName = subjectName;
        this.subtopicName = subtopicName;
        this.attempted = attempted;
        this.correct = correct;
    }

    public String getSubjectName() { return subjectName; }
    public String getSubtopicName() { return subtopicName; }
    public int getAttempted() { return attempted; }
    public int getCorrect() { return correct; }

    public double getAccuracy() {
        return attempted == 0 ? 0 : correct * 100.0 / attempted;
    }

    /** Weakest first; ties broken by more attempts (more evidence) first. */
    @Override
    public int compareTo(TopicStat o) {
        int c = Double.compare(getAccuracy(), o.getAccuracy());
        return c != 0 ? c : Integer.compare(o.attempted, attempted);
    }
}
