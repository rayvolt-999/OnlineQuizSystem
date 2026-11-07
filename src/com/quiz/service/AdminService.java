package com.quiz.service;

import com.quiz.dao.DAOFactory;
import com.quiz.exception.DataAccessException;
import com.quiz.exception.ValidationException;
import com.quiz.model.Difficulty;
import com.quiz.model.Question;
import com.quiz.model.QuizResult;
import com.quiz.model.Subject;
import com.quiz.model.Subtopic;
import com.quiz.model.User;
import com.quiz.util.CsvUtil;
import com.quiz.util.QuestionBankFormat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Subject / subtopic / question management, question-bank import & export, results export. */
public class AdminService {

    private final DAOFactory dao;

    public AdminService(DAOFactory dao) {
        this.dao = dao;
    }

    // ================================================================ subjects

    public List<Subject> getSubjects() throws DataAccessException {
        return dao.subjects().findAll();
    }

    public Subject saveSubject(Subject s) throws ValidationException, DataAccessException {
        if (s.getName() == null || s.getName().trim().isEmpty()) throw new ValidationException("Subject name is required.");
        if (s.getName().trim().length() > 60) throw new ValidationException("Subject name must be at most 60 characters.");
        if (s.getShortCode() == null || !s.getShortCode().trim().matches("[A-Za-z0-9+#$_/]{1,4}")) {
            throw new ValidationException("Badge text must be 1-4 letters or digits (e.g. DS, PY, OS).");
        }
        if (s.getColor() == null || !s.getColor().matches("#[0-9A-Fa-f]{6}")) {
            throw new ValidationException("Choose a colour.");
        }
        s.setName(s.getName().trim());
        s.setShortCode(s.getShortCode().trim().toUpperCase());
        return dao.subjects().save(s);
    }

    public void deleteSubject(Subject s) throws DataAccessException {
        dao.subjects().delete(s.getId());
    }

    // ================================================================ subtopics

    public List<Subtopic> getSubtopics(Subject s) throws DataAccessException {
        return dao.subtopics().findBySubject(s.getId());
    }

    public Subtopic saveSubtopic(Subtopic t) throws ValidationException, DataAccessException {
        if (t.getSubjectId() <= 0) throw new ValidationException("Choose a subject for the subtopic.");
        if (t.getName() == null || t.getName().trim().isEmpty()) throw new ValidationException("Subtopic name is required.");
        if (t.getName().trim().length() > 60) throw new ValidationException("Subtopic name must be at most 60 characters.");
        t.setName(t.getName().trim());
        return dao.subtopics().save(t);
    }

    public void deleteSubtopic(Subtopic t) throws DataAccessException {
        dao.subtopics().delete(t.getId());
    }

    // ================================================================ questions

    public List<Question> getQuestions(Subject subject, Subtopic subtopic, Difficulty difficulty) throws DataAccessException {
        return dao.questions().find(subject == null ? 0 : subject.getId(), subtopic == null ? 0 : subtopic.getId(), difficulty);
    }

    public Question saveQuestion(Question q) throws ValidationException, DataAccessException {
        validate(q);
        return dao.questions().save(q);
    }

    public void deleteQuestion(Question q) throws DataAccessException {
        dao.questions().delete(q.getId());
    }

    public static void validate(Question q) throws ValidationException {
        if (q.getSubtopicId() <= 0) throw new ValidationException("Choose a subtopic for the question.");
        if (q.getDifficulty() == null) throw new ValidationException("Choose a difficulty.");
        if (q.getText() == null || q.getText().trim().isEmpty()) throw new ValidationException("Question text is required.");
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < Question.OPTION_COUNT; i++) {
            String o = q.getOption(i);
            if (o == null || o.trim().isEmpty()) throw new ValidationException("Option " + Question.label(i) + " is empty.");
            if (!seen.add(o.trim())) {   // case-sensitive: "G" and "g" can be different outputs
                throw new ValidationException("Options must be different (option " + Question.label(i) + " repeats).");
            }
        }
        if (q.getCorrectIndex() < 0 || q.getCorrectIndex() >= Question.OPTION_COUNT) {
            throw new ValidationException("Select the correct option (A-D).");
        }
        if (q.hasCode() && q.getCode().length() > 4000) throw new ValidationException("Code snippet is too long (max 4000 characters).");
    }

    // ================================================================ import / export (*.qb)

    /** Outcome of an import. */
    public static class ImportReport {
        public int imported;
        public int subjectsCreated;
        public int subtopicsCreated;
        public final List<String> errors = new ArrayList<>();
    }

    public ImportReport importBank(Path file) throws DataAccessException {
        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return importBank(r, file.getFileName().toString());
        } catch (IOException e) {
            throw new DataAccessException("Could not read " + file.getFileName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Imports a question bank. Missing subjects/subtopics are created; invalid questions are skipped
     * and reported; valid questions are inserted in a single transaction.
     */
    public ImportReport importBank(BufferedReader reader, String sourceName) throws DataAccessException {
        ImportReport rep = new ImportReport();
        QuestionBankFormat.ParseResult parsed;
        try {
            parsed = QuestionBankFormat.parse(reader, sourceName);
        } catch (IOException e) {
            throw new DataAccessException("Could not read " + sourceName + ": " + e.getMessage(), e);
        }
        rep.errors.addAll(parsed.errors);

        Map<String, Subject> subjects = new HashMap<>();
        for (Subject s : dao.subjects().findAll()) subjects.put(s.getName().toLowerCase(), s);
        Map<String, Subtopic> subtopics = new HashMap<>();

        // create subjects and subtopics in file order so the display order matches the file
        for (Map.Entry<String, List<String>> e : parsed.subtopicOrder.entrySet()) {
            Subject s = subjects.get(e.getKey().toLowerCase());
            if (s == null) {
                QuestionBankFormat.SubjectDef d = parsed.subjects.get(e.getKey());
                s = dao.subjects().save(new Subject(0, d.name, d.code, d.description, d.color));
                subjects.put(s.getName().toLowerCase(), s);
                rep.subjectsCreated++;
            }
            for (String tn : e.getValue()) {
                String key = s.getId() + "|" + tn.toLowerCase();
                Subtopic t = dao.subtopics().findByName(s.getId(), tn).orElse(null);
                if (t == null) {
                    t = dao.subtopics().save(new Subtopic(0, s.getId(), tn));
                    rep.subtopicsCreated++;
                }
                subtopics.put(key, t);
            }
        }

        List<Question> valid = new ArrayList<>();
        for (QuestionBankFormat.Entry en : parsed.entries) {
            Subject s = subjects.get(en.subject.toLowerCase());
            Subtopic t = s == null ? null : subtopics.get(s.getId() + "|" + en.subtopic.toLowerCase());
            if (t == null) {
                rep.errors.add(sourceName + ":" + en.line + ": unknown subtopic '" + en.subtopic + "'");
                continue;
            }
            en.question.setSubtopicId(t.getId());
            try {
                validate(en.question);
                valid.add(en.question);
            } catch (ValidationException ex) {
                rep.errors.add(sourceName + ":" + en.line + ": " + ex.getMessage());
            }
        }
        if (!valid.isEmpty()) rep.imported = dao.questions().saveAll(valid);
        return rep;
    }

    /** Exports one subject (or all when null) to a .qb file. Returns the number of questions written. */
    public int exportBank(Path file, Subject subjectOrNull) throws DataAccessException {
        List<Question> qs = getQuestions(subjectOrNull, null, null);
        Map<String, QuestionBankFormat.SubjectDef> headers = new LinkedHashMap<>();
        for (Subject s : dao.subjects().findAll()) {
            headers.put(s.getName(), new QuestionBankFormat.SubjectDef(s.getName(), s.getShortCode(), s.getColor(), s.getDescription()));
        }
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            QuestionBankFormat.write(w, qs, headers);
            return qs.size();
        } catch (IOException e) {
            throw new DataAccessException("Could not write " + file.getFileName() + ": " + e.getMessage(), e);
        }
    }

    /** Writes results as CSV (opens in Excel / LibreOffice). */
    public static void exportResults(Path file, List<QuizResult> results) throws DataAccessException {
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write('\uFEFF');   // byte-order mark so Excel opens the UTF-8 file with the right characters
            w.write(CsvUtil.toLine("id", "student", "subject", "subtopic", "mode", "date", "questions", "correct",
                    "wrong", "unanswered", "hints_used", "score", "max_score", "percentage", "grade", "time_taken",
                    "auto_submitted"));
            w.newLine();
            for (QuizResult r : results) {
                w.write(CsvUtil.toLine(r.getId(), CsvUtil.safeText(r.getUserName()), CsvUtil.safeText(r.getSubjectName()),
                        CsvUtil.safeText(r.getSubtopicName()),
                        r.getDifficulty().getLabel(), r.getTakenAt().format(QuizResult.DISPLAY_FMT),
                        r.getTotalQuestions(), r.getCorrect(), r.getWrong(), r.getUnanswered(), r.getHintsUsed(),
                        fmt(r.getScore()), fmt(r.getMaxScore()), String.format(java.util.Locale.ROOT, "%.1f", r.getPercentage()), r.getGrade(),
                        QuizResult.formatDuration(r.getTimeTakenSeconds()), r.isAutoSubmitted() ? "yes" : "no"));
                w.newLine();
            }
        } catch (IOException e) {
            throw new DataAccessException("Could not write " + file.getFileName() + ": " + e.getMessage(), e);
        }
    }

    public static String fmt(double d) {
        // Locale.ROOT: always a dot as decimal separator (e.g. 7.50), whatever the computer's language
        return d == Math.rint(d) ? String.valueOf((long) d) : String.format(java.util.Locale.ROOT, "%.2f", d);
    }

    // ================================================================ users

    public List<User> getUsers() throws DataAccessException {
        return dao.users().findAll();
    }

    /** Creates another admin account (students register themselves from the login screen). */
    public User createAdmin(String username, String fullName, String password) throws ValidationException, DataAccessException {
        if (fullName == null || fullName.trim().isEmpty()) throw new ValidationException("Full name is required.");
        if (username == null || !username.trim().matches("[A-Za-z0-9_.]{3,20}")) {
            throw new ValidationException("Username must be 3-20 characters: letters, digits, '_' or '.'.");
        }
        if (password == null || password.length() < 4) throw new ValidationException("Password must be at least 4 characters.");
        if (dao.users().exists(username.trim())) throw new ValidationException("Username already taken.");
        return dao.users().create(username.trim(), fullName.trim(), password, User.Role.ADMIN);
    }
}
