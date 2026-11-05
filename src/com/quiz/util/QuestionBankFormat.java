package com.quiz.util;

import com.quiz.model.Difficulty;
import com.quiz.model.Question;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reader/writer for the plain-text question bank format (*.qb), written with Java I/O.
 * The format is easy to type by hand and keeps code snippets readable:
 *
 * <pre>
 * # comment
 * &#64;subject Data Structures | DS | #2563EB | Arrays, lists, trees and graphs
 * &#64;subtopic Linked Lists
 *
 * [MEDIUM]
 * Q: What is the output?
 * CODE java
 * public class Main { ... }
 * END
 * A) 1 2 3
 * B) 3 2 1
 * C) 1 3
 * D) Compilation error
 * ANSWER: B
 * HINT: Trace the loop for the first two nodes.
 * EXPLAIN: The list is reversed in place, so ...
 * </pre>
 *
 * Lines that do not start with a keyword continue the previous field (question, hint or explanation).
 */
public final class QuestionBankFormat {

    private QuestionBankFormat() {
    }

    /** Subject header (from an &#64;subject line). */
    public static class SubjectDef {
        public final String name;
        public final String code;
        public final String color;
        public final String description;

        public SubjectDef(String name, String code, String color, String description) {
            this.name = name;
            this.code = code;
            this.color = color;
            this.description = description;
        }
    }

    /** One parsed question together with where it belongs. */
    public static class Entry {
        public final String subject;
        public final String subtopic;
        public final Question question;
        public final int line;

        Entry(String subject, String subtopic, Question question, int line) {
            this.subject = subject;
            this.subtopic = subtopic;
            this.question = question;
            this.line = line;
        }
    }

    /** Everything read from one file. Invalid questions are listed in {@code errors} and skipped. */
    public static class ParseResult {
        public final Map<String, SubjectDef> subjects = new LinkedHashMap<>();
        public final Map<String, List<String>> subtopicOrder = new LinkedHashMap<>();
        public final List<Entry> entries = new ArrayList<>();
        public final List<String> errors = new ArrayList<>();
    }

    private enum Field { NONE, QUESTION, OPTION, HINT, EXPLAIN }

    public static ParseResult parse(BufferedReader reader, String source) throws IOException {
        ParseResult out = new ParseResult();
        String subject = null;
        String subtopic = null;
        Draft draft = null;
        Field last = Field.NONE;
        int lastOption = -1;
        StringBuilder code = null;
        String line;
        int lineNo = 0;

        while ((line = reader.readLine()) != null) {
            lineNo++;
            if (lineNo == 1 && line.startsWith("﻿")) line = line.substring(1);

            if (code != null) {                       // inside CODE ... END: keep verbatim
                if (line.trim().equals("END")) {
                    draft.code = stripTrailingNewline(code.toString());
                    code = null;
                } else {
                    code.append(line).append('\n');
                }
                continue;
            }

            String t = line.trim();
            if (t.isEmpty() || t.startsWith("#")) continue;

            if (t.startsWith("@subject")) {
                finish(draft, out, source);
                draft = null;
                String[] parts = t.substring(8).split("\\|", -1);
                subject = parts[0].trim();
                String sc = parts.length > 1 ? parts[1].trim() : abbreviate(subject);
                String color = parts.length > 2 ? parts[2].trim() : "#2563EB";
                String desc = parts.length > 3 ? parts[3].trim() : "";
                out.subjects.putIfAbsent(subject, new SubjectDef(subject, sc, color, desc));
                out.subtopicOrder.putIfAbsent(subject, new ArrayList<>());
                subtopic = null;
            } else if (t.startsWith("@subtopic")) {
                finish(draft, out, source);
                draft = null;
                subtopic = t.substring(9).trim();
                if (subject != null && !out.subtopicOrder.get(subject).contains(subtopic)) {
                    out.subtopicOrder.get(subject).add(subtopic);
                }
            } else if (t.matches("\\[(EASY|MEDIUM|HARD)\\]")) {
                finish(draft, out, source);
                draft = new Draft(subject, subtopic, Difficulty.parse(t.substring(1, t.length() - 1)), lineNo);
                last = Field.NONE;
            } else if (draft == null) {
                out.errors.add(source + ":" + lineNo + ": text outside a question (expected [EASY], [MEDIUM] or [HARD])");
            } else if (t.startsWith("Q:")) {
                draft.text = t.substring(2).trim();
                last = Field.QUESTION;
            } else if (t.equals("CODE") || t.startsWith("CODE ")) {
                draft.lang = t.length() > 4 ? t.substring(5).trim() : null;
                code = new StringBuilder();
                last = Field.NONE;
            } else if (t.length() >= 2 && t.charAt(1) == ')' && t.charAt(0) >= 'A' && t.charAt(0) <= 'D') {
                lastOption = t.charAt(0) - 'A';
                draft.options[lastOption] = t.substring(2).trim();
                last = Field.OPTION;
            } else if (t.startsWith("ANSWER:")) {
                draft.answer = t.substring(7).trim();
                last = Field.NONE;
            } else if (t.startsWith("HINT:")) {
                draft.hint = t.substring(5).trim();
                last = Field.HINT;
            } else if (t.startsWith("EXPLAIN:")) {
                draft.explain = t.substring(8).trim();
                last = Field.EXPLAIN;
            } else {                                  // continuation of the previous field
                switch (last) {
                    case QUESTION: draft.text += "\n" + t; break;
                    case OPTION: draft.options[lastOption] += " " + t; break;
                    case HINT: draft.hint += " " + t; break;
                    case EXPLAIN: draft.explain += " " + t; break;
                    default:
                        out.errors.add(source + ":" + lineNo + ": unrecognised line: " + t);
                }
            }
        }
        if (code != null) {
            out.errors.add(source + ":" + draft.line + ": CODE block is missing its END line");
        } else {
            finish(draft, out, source);
        }
        return out;
    }

    private static void finish(Draft d, ParseResult out, String source) {
        if (d == null) return;
        String where = source + ":" + d.line + ": ";
        if (d.subject == null || d.subtopic == null) {
            out.errors.add(where + "question appears before @subject / @subtopic");
            return;
        }
        if (d.text == null || d.text.isEmpty()) { out.errors.add(where + "missing Q:"); return; }
        for (int i = 0; i < 4; i++) {
            if (d.options[i] == null || d.options[i].isEmpty()) {
                out.errors.add(where + "missing option " + (char) ('A' + i) + ")");
                return;
            }
        }
        int correct = d.answer == null || d.answer.length() != 1 ? -1 : "ABCD".indexOf(Character.toUpperCase(d.answer.charAt(0)));
        if (correct < 0) { out.errors.add(where + "ANSWER must be A, B, C or D"); return; }
        Question q = new Question(0, 0, d.difficulty, d.text, d.options, correct);
        q.setCode(d.code);
        q.setCodeLang(d.lang);
        q.setHint(d.hint);
        q.setExplanation(d.explain);
        q.setSubjectName(d.subject);
        q.setSubtopicName(d.subtopic);
        out.entries.add(new Entry(d.subject, d.subtopic, q, d.line));
    }

    private static String stripTrailingNewline(String s) {
        return s.endsWith("\n") ? s.substring(0, s.length() - 1) : s;
    }

    private static String abbreviate(String name) {
        StringBuilder sb = new StringBuilder();
        for (String w : name.split("\\s+")) if (!w.isEmpty() && Character.isLetter(w.charAt(0))) sb.append(w.charAt(0));
        return sb.length() == 0 ? "?" : sb.toString().toUpperCase();
    }

    /** Mutable holder while a question is being read. */
    private static final class Draft {
        final String subject;
        final String subtopic;
        final Difficulty difficulty;
        final int line;
        String text;
        String code;
        String lang;
        final String[] options = new String[4];
        String answer;
        String hint;
        String explain;

        Draft(String subject, String subtopic, Difficulty difficulty, int line) {
            this.subject = subject;
            this.subtopic = subtopic;
            this.difficulty = difficulty;
            this.line = line;
        }
    }

    // ------------------------------------------------------------------ writing

    /**
     * Writes questions grouped by subject and subtopic (the list should already be sorted that way).
     * @param subjectHeaders optional header details per subject name (may be empty)
     */
    public static void write(Writer w, List<Question> questions, Map<String, SubjectDef> subjectHeaders) throws IOException {
        String nl = System.lineSeparator();
        w.write("# Question bank exported by Online Quiz System" + nl);
        String curSubject = null;
        String curSubtopic = null;
        for (Question q : questions) {
            if (!q.getSubjectName().equals(curSubject)) {
                curSubject = q.getSubjectName();
                curSubtopic = null;
                SubjectDef h = subjectHeaders.get(curSubject);
                w.write(nl + "@subject " + curSubject + (h == null ? "" : " | " + h.code + " | " + h.color + " | "
                        + (h.description == null ? "" : h.description)) + nl);
            }
            if (!q.getSubtopicName().equals(curSubtopic)) {
                curSubtopic = q.getSubtopicName();
                w.write("@subtopic " + curSubtopic + nl);
            }
            w.write(nl + "[" + q.getDifficulty().name() + "]" + nl);
            w.write("Q: " + q.getText().replace("\n", nl) + nl);
            if (q.hasCode()) {
                w.write("CODE" + (q.getCodeLang() == null ? "" : " " + q.getCodeLang()) + nl);
                w.write(q.getCode().replace("\n", nl) + nl);
                w.write("END" + nl);
            }
            for (int i = 0; i < Question.OPTION_COUNT; i++) {
                w.write(Question.LABELS[i] + ") " + q.getOption(i).replace("\n", " ") + nl);
            }
            w.write("ANSWER: " + Question.label(q.getCorrectIndex()) + nl);
            if (q.getHint() != null) w.write("HINT: " + q.getHint().replace("\n", " ") + nl);
            if (q.getExplanation() != null) w.write("EXPLAIN: " + q.getExplanation().replace("\n", " ") + nl);
        }
    }
}
