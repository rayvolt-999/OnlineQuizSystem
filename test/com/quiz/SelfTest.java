package com.quiz;

import com.quiz.dao.DAOFactory;
import com.quiz.db.DataSeeder;
import com.quiz.db.DatabaseManager;
import com.quiz.exception.AuthenticationException;
import com.quiz.exception.DataAccessException;
import com.quiz.exception.ValidationException;
import com.quiz.model.AnswerRecord;
import com.quiz.model.Difficulty;
import com.quiz.model.Question;
import com.quiz.model.QuizResult;
import com.quiz.model.Subject;
import com.quiz.model.Subtopic;
import com.quiz.model.TopicStat;
import com.quiz.model.User;
import com.quiz.service.AdminService;
import com.quiz.service.AuthService;
import com.quiz.service.QuizService;
import com.quiz.service.QuizSession;
import com.quiz.util.CsvUtil;
import com.quiz.util.QuestionBankFormat;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Dependency-free test runner (no JUnit needed). Run with ./test.sh (or test.bat).
 * Uses temporary database files so the real data/quiz.db is never touched.
 */
public final class SelfTest {

    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        testMigrationFromVersion1();

        File tmp = File.createTempFile("quiztest", ".db");
        tmp.delete();
        tmp.deleteOnExit();
        DatabaseManager db = DatabaseManager.useDatabase(tmp.getPath());
        db.createSchema();
        DAOFactory dao = new DAOFactory(db);
        List<String> seedProblems = DataSeeder.seedIfEmpty(dao);

        testCsv();
        testBankFormat();
        testSeedData(dao, seedProblems);
        testAuth(dao);
        testDifficultySettings();
        testScoringAndHints();
        testQuizService(dao);
        testValidation();
        testCrudAndCascade(dao);
        testImportExport(dao);

        System.out.println();
        System.out.println("Passed: " + passed + "   Failed: " + failed);
        if (failed > 0) System.exit(1);
    }

    // ------------------------------------------------------------------ migration

    private static void testMigrationFromVersion1() throws Exception {
        File f = File.createTempFile("quizv1", ".db");
        f.delete();
        f.deleteOnExit();
        DatabaseManager db = DatabaseManager.useDatabase(f.getPath());
        try (Connection c = db.getConnection(); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL UNIQUE COLLATE NOCASE,"
                    + " full_name TEXT NOT NULL, password_hash TEXT NOT NULL, salt TEXT NOT NULL, role TEXT NOT NULL,"
                    + " created_at TEXT NOT NULL DEFAULT (datetime('now','localtime')))");
            st.execute("CREATE TABLE topics (id INTEGER PRIMARY KEY, name TEXT)");
            st.execute("CREATE TABLE questions (id INTEGER PRIMARY KEY, topic_id INTEGER, question_text TEXT)");
            st.execute("CREATE TABLE results (id INTEGER PRIMARY KEY, user_id INTEGER, topic_name TEXT)");
        }
        new DAOFactory(db).users().create("oldstudent", "Old Student", "pass", User.Role.STUDENT);
        db.createSchema();
        DAOFactory dao = new DAOFactory(db);
        check("migration: v1 account kept", dao.users().authenticate("oldstudent", "pass").isPresent());
        try (Connection c = db.getConnection(); Statement st = c.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM sqlite_master WHERE name='topics'");
            rs.next();
            check("migration: old topics table removed", rs.getInt(1) == 0);
            rs = st.executeQuery("PRAGMA user_version");
            rs.next();
            check("migration: schema version is 2", rs.getInt(1) == DatabaseManager.SCHEMA_VERSION);
        }
        DataSeeder.seedIfEmpty(dao);
        check("migration: new bank seeded", dao.questions().count() > 0 && dao.users().count() == 1);
    }

    // ------------------------------------------------------------------ file formats

    private static void testCsv() throws Exception {
        String csv = "a,b,c\n\"x, y\",\"he said \"\"hi\"\"\",\"multi\nline\"\r\n\n last ,,\n";
        List<List<String>> rows = CsvUtil.readAll(new BufferedReader(new StringReader(csv)));
        check("CSV: row count", rows.size() == 3);
        check("CSV: quoted comma", rows.get(1).get(0).equals("x, y"));
        check("CSV: escaped quotes", rows.get(1).get(1).equals("he said \"hi\""));
        check("CSV: newline in quotes", rows.get(1).get(2).equals("multi\nline"));
        check("CSV: escape round-trip", CsvUtil.toLine("a,b", "q\"", "plain").equals("\"a,b\",\"q\"\"\",plain"));
        check("CSV: formula-looking text is neutralised", CsvUtil.safeText("=HYPERLINK(\"x\")").equals("'=HYPERLINK(\"x\")")
                && CsvUtil.safeText("@SUM(A1)").startsWith("'") && CsvUtil.safeText("-2+3").startsWith("'"));
        check("CSV: plain text and numbers untouched", CsvUtil.safeText("Asha Rao").equals("Asha Rao")
                && CsvUtil.safeText("-1.5").equals("-1.5") && CsvUtil.safeText("") .isEmpty());
        java.util.Locale before = java.util.Locale.getDefault();
        java.util.Locale.setDefault(java.util.Locale.GERMANY);
        check("format: marks use a dot in every locale", com.quiz.service.AdminService.fmt(7.5).equals("7.50"));
        java.util.Locale.setDefault(before);
        System.setProperty("easy.questions", "0");
        System.setProperty("easy.minutes", "-5");
        check("config: question count and minutes are clamped", com.quiz.model.Difficulty.EASY.getQuestionCount() == 1
                && com.quiz.model.Difficulty.EASY.getMinutes() == 0);
        System.clearProperty("easy.questions");
        System.clearProperty("easy.minutes");
    }

    private static final String SAMPLE_BANK = String.join("\n",
            "# comment",
            "@subject Sample Subject | SS | #123456 | For tests",
            "@subtopic Basics",
            "[EASY]",
            "Q: First question",
            "that continues on a second line",
            "A) one",
            "B) two",
            "C) three",
            "D) four",
            "ANSWER: b",
            "",
            "[MEDIUM]",
            "Q: What is the output?",
            "CODE java",
            "public class Main {",
            "    public static void main(String[] a) {",
            "        System.out.println(1 + 1);",
            "    }",
            "}",
            "END",
            "A) 2",
            "B) 11",
            "C) Compilation error",
            "D) 0",
            "ANSWER: A",
            "HINT: Both operands are int.",
            "EXPLAIN: Integer addition.",
            "@subtopic Errors",
            "[HARD]",
            "Q: Missing an option",
            "A) x",
            "B) y",
            "C) z",
            "ANSWER: A",
            "[HARD]",
            "Q: Bad answer letter",
            "A) x",
            "B) y",
            "C) z",
            "D) w",
            "ANSWER: E",
            "");

    private static void testBankFormat() throws Exception {
        QuestionBankFormat.ParseResult r = QuestionBankFormat.parse(new BufferedReader(new StringReader(SAMPLE_BANK)), "t.qb");
        check("QB: 2 valid questions parsed", r.entries.size() == 2);
        check("QB: 2 errors reported", r.errors.size() == 2);
        check("QB: subject header read", r.subjects.get("Sample Subject").code.equals("SS")
                && r.subjects.get("Sample Subject").color.equals("#123456"));
        check("QB: subtopics in file order", r.subtopicOrder.get("Sample Subject").equals(Arrays.asList("Basics", "Errors")));
        Question q1 = r.entries.get(0).question;
        Question q2 = r.entries.get(1).question;
        check("QB: continuation line joined", q1.getText().equals("First question\nthat continues on a second line"));
        check("QB: lower-case answer accepted", q1.getCorrectIndex() == 1 && q1.getDifficulty() == Difficulty.EASY);
        check("QB: code kept verbatim with indentation", q2.hasCode() && q2.getCode().contains("        System.out.println(1 + 1);")
                && q2.getCodeLang().equals("java") && !q2.getCode().endsWith("\n"));
        check("QB: hint and explanation", "Both operands are int.".equals(q2.getHint()) && "Integer addition.".equals(q2.getExplanation()));
        check("QB: error mentions line number", r.errors.get(0).startsWith("t.qb:"));

        StringWriter w = new StringWriter();
        QuestionBankFormat.write(w, Arrays.asList(q1, q2), new LinkedHashMap<>(r.subjects));
        QuestionBankFormat.ParseResult again = QuestionBankFormat.parse(new BufferedReader(new StringReader(w.toString())), "rt.qb");
        check("QB: write -> parse round trip", again.errors.isEmpty() && again.entries.size() == 2
                && again.entries.get(1).question.getCode().equals(q2.getCode())
                && again.entries.get(0).question.getText().equals(q1.getText()));
    }

    // ------------------------------------------------------------------ seed data

    private static void testSeedData(DAOFactory dao, List<String> problems) throws Exception {
        List<Subject> subjects = dao.subjects().findAll();
        check("seed: bundled banks load without errors " + problems, problems.isEmpty());
        check("seed: 8 subjects", subjects.size() == 8);
        List<String> names = subjects.stream().map(Subject::getName).collect(Collectors.toList());
        check("seed: required subjects present", names.containsAll(Arrays.asList("Data Structures", "Java Programming",
                "Python Programming", "Linux Commands", "Computer Networks", "OOP Concepts")));
        check("seed: 460 questions", dao.questions().count() == 460);
        boolean full = subjects.stream().allMatch(s -> s.getQuestionCount(Difficulty.EASY) >= Difficulty.EASY.getQuestionCount()
                && s.getQuestionCount(Difficulty.MEDIUM) >= Difficulty.MEDIUM.getQuestionCount()
                && s.getQuestionCount(Difficulty.HARD) >= Difficulty.HARD.getQuestionCount());
        check("seed: every subject can fill a full Easy/Medium/Hard test", full);
        boolean topicsOk = true;
        int subtopicTotal = 0;
        for (Subject s : subjects) {
            for (Subtopic t : dao.subtopics().findBySubject(s.getId())) {
                subtopicTotal++;
                for (Difficulty d : Difficulty.values()) topicsOk &= t.getQuestionCount(d) > 0;
            }
        }
        check("seed: every subtopic has Easy, Medium and Hard questions", topicsOk);
        check("seed: at least 5 subtopics per subject", subtopicTotal >= 40);
        List<Question> all = dao.questions().find(0, 0, null);
        check("seed: every Medium/Hard question has a hint",
                all.stream().filter(q -> q.getDifficulty() != Difficulty.EASY).allMatch(Question::hasHint));
        check("seed: Medium/Hard include code snippets",
                all.stream().filter(q -> q.getDifficulty() != Difficulty.EASY && q.hasCode()).count() >= 100);
        check("seed: Easy questions are theory (no code)",
                all.stream().filter(q -> q.getDifficulty() == Difficulty.EASY).noneMatch(Question::hasCode));
        DataSeeder.seedIfEmpty(dao);
        check("seed: idempotent", dao.questions().count() == 460 && dao.users().count() == 2);
    }

    // ------------------------------------------------------------------ auth

    private static void testAuth(DAOFactory dao) throws Exception {
        AuthService auth = new AuthService(dao.users());
        User admin = auth.login("admin", "admin123");
        check("auth: admin login", admin.isAdmin());
        check("auth: username is case-insensitive", auth.login("ADMIN", "admin123").getId() == admin.getId());
        expect("auth: wrong password rejected", AuthenticationException.class, () -> auth.login("admin", "nope"));
        expect("auth: blank login rejected", AuthenticationException.class, () -> auth.login(" ", ""));
        User s = auth.registerStudent("ravi_k", "Ravi Kumar", "pass1", "pass1");
        check("auth: register student", s.getRole() == User.Role.STUDENT && s.getId() > 0);
        expect("auth: duplicate username rejected", ValidationException.class, () -> auth.registerStudent("RAVI_K", "X", "pass1", "pass1"));
        expect("auth: password mismatch rejected", ValidationException.class, () -> auth.registerStudent("newuser", "X", "pass1", "pass2"));
        expect("auth: bad username rejected", ValidationException.class, () -> auth.registerStudent("a b", "X", "pass1", "pass1"));
    }

    // ------------------------------------------------------------------ modes and scoring

    private static void testDifficultySettings() {
        check("mode: Easy = 20 questions / 10 min / 1 mark / no hints", Difficulty.EASY.getQuestionCount() == 20
                && Difficulty.EASY.getMinutes() == 10 && Difficulty.EASY.getMarks() == 1 && !Difficulty.EASY.isHintsAllowed());
        check("mode: Medium = 10 questions / 10 min / 2 marks / hints", Difficulty.MEDIUM.getQuestionCount() == 10
                && Difficulty.MEDIUM.getMinutes() == 10 && Difficulty.MEDIUM.getMarks() == 2 && Difficulty.MEDIUM.isHintsAllowed());
        check("mode: Hard = 5 questions / 10 min / 3 marks / hints", Difficulty.HARD.getQuestionCount() == 5
                && Difficulty.HARD.getMinutes() == 10 && Difficulty.HARD.getMarks() == 3 && Difficulty.HARD.isHintsAllowed());
    }

    private static List<Question> sample(Difficulty d) {
        List<Question> qs = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Question q = new Question(i + 1, 1, d, "Q" + i, new String[]{"a", "b", "c", "d"}, i % 4);
            q.setHint(i == 4 ? null : "hint " + i);
            q.setSubtopicName("T");
            qs.add(q);
        }
        return qs;
    }

    private static void testScoringAndHints() {
        User u = new User(1, "u", "U", User.Role.STUDENT);
        Subject subj = new Subject(1, "S", "S", "", "#000000");
        QuizSession s = new QuizSession(u, subj, null, Difficulty.MEDIUM, sample(Difficulty.MEDIUM), 600);
        s.goTo(0); s.selectAnswer(0);                          // correct
        s.goTo(1); s.selectAnswer(3);                          // wrong
        s.goTo(2); s.selectAnswer(2); s.revealHint();          // correct, hint used
        s.goTo(3); s.selectAnswer(1); s.clearAnswer();         // cleared -> unanswered
        s.goTo(4);                                             // never answered, no hint text
        check("session: unanswered numbers are 4 and 5", s.unansweredNumbers().equals(Arrays.asList(4, 5)));
        check("session: hint unavailable when question has none", !s.isHintAvailable(4));
        expect("session: revealing a missing hint fails", IllegalStateException.class, s::revealHint);
        s.toggleMarked();
        check("session: mark for review", s.isMarked(4) && s.markedCount() == 1);
        check("session: hint recorded", s.isHintShown(2) && s.hintsUsed() == 1);
        QuizResult r = s.finish(2, 0, false);
        check("score: 2 correct, 1 wrong, 2 unanswered", r.getCorrect() == 2 && r.getWrong() == 1 && r.getUnanswered() == 2);
        check("score: 2 marks each -> 4 / 10 = 40%", r.getScore() == 4 && r.getMaxScore() == 10 && Math.abs(r.getPercentage() - 40) < 1e-9);
        check("score: hints are free but tracked", r.getHintsUsed() == 1 && r.getAnswers().get(2).isHintUsed()
                && r.getAnswers().get(2).getStatus() == AnswerRecord.Status.CORRECT);
        check("score: accuracy = correct / attempted", Math.abs(r.getAccuracy() - 200.0 / 3) < 1e-9);
        check("score: grade D at 40%", r.getGrade().equals("D") && r.isPassed());
        expect("session: cannot answer after submit", IllegalStateException.class, () -> s.selectAnswer(1));

        QuizSession easy = new QuizSession(u, subj, null, Difficulty.EASY, sample(Difficulty.EASY), 600);
        check("session: no hints in Easy mode", !easy.isHintAvailable(0));
        expect("session: Easy hint request rejected", IllegalStateException.class, easy::revealHint);

        QuizSession neg = new QuizSession(u, subj, null, Difficulty.EASY, sample(Difficulty.EASY), 0);
        neg.goTo(0); neg.selectAnswer(1);
        neg.goTo(1); neg.selectAnswer(0);
        QuizResult nr = neg.finish(1, 0.25, false);
        check("score: negative marking -0.5, percentage clamped to 0", nr.getScore() == -0.5 && nr.getPercentage() == 0);
        check("session: no time limit when 0", !neg.hasTimeLimit());
    }

    private static void testQuizService(DAOFactory dao) throws Exception {
        QuizService svc = new QuizService(dao, new Random(7));
        User student = new AuthService(dao.users()).login("student", "student123");
        Subject ds = dao.subjects().findByName("Data Structures").orElseThrow(IllegalStateException::new);

        QuizSession easy = svc.startQuiz(student, ds, null, Difficulty.EASY);
        check("quiz: Easy subject test has 20 questions", easy.size() == 20);
        check("quiz: Easy questions are all EASY", allMatch(easy, q -> q.getDifficulty() == Difficulty.EASY));
        check("quiz: Easy time limit 10 minutes", easy.getTimeLimitSeconds() == 600);
        check("quiz: questions are distinct", distinct(easy) == 20);
        Subtopic ll = dao.subtopics().findByName(ds.getId(), "Linked Lists").orElseThrow(IllegalStateException::new);
        check("quiz: mixed test spans several subtopics", easy.size() > 0 && countSubtopics(easy) > 1);

        QuizSession med = svc.startQuiz(student, ds, null, Difficulty.MEDIUM);
        QuizSession hard = svc.startQuiz(student, ds, null, Difficulty.HARD);
        check("quiz: Medium has 10, Hard has 5", med.size() == 10 && hard.size() == 5);
        check("quiz: Medium/Hard difficulty respected", allMatch(med, q -> q.getDifficulty() == Difficulty.MEDIUM)
                && allMatch(hard, q -> q.getDifficulty() == Difficulty.HARD));

        QuizSession topic = svc.startQuiz(student, ds, ll, Difficulty.EASY);
        check("quiz: subtopic test only uses that subtopic", allMatch(topic, q -> q.getSubtopicId() == ll.getId()));
        check("quiz: subtopic test limited to available questions", topic.size() == ll.getQuestionCount(Difficulty.EASY)
                && topic.size() < 20 && QuizService.plannedQuestionCount(topic.size(), Difficulty.EASY) == topic.size());

        for (int i = 0; i < med.size(); i++) {
            med.goTo(i);
            if (i < 7) med.selectAnswer(med.getCurrentQuestion().getCorrectIndex());
            else if (i == 7) med.selectAnswer((med.getCurrentQuestion().getCorrectIndex() + 1) % 4);
            if (i == 0) med.revealHint();
        }
        QuizResult r = svc.submit(med, false);
        check("quiz: result saved", r.getId() > 0);
        check("quiz: Medium scoring 7 x 2 = 14 / 20", r.getScore() == 14 && r.getMaxScore() == 20 && r.getUnanswered() == 2);
        QuizResult loaded = svc.historyFor(student).get(0);
        check("history: values persisted", loaded.getId() == r.getId() && loaded.getDifficulty() == Difficulty.MEDIUM
                && loaded.getHintsUsed() == 1 && loaded.getSubtopicName().equals(QuizResult.ALL_SUBTOPICS)
                && loaded.getTitle().equals("Data Structures"));
        svc.loadAnswers(loaded);
        check("history: answers with code, hint and explanation persisted", loaded.getAnswers().size() == 10
                && loaded.getAnswers().get(0).isHintUsed()
                && loaded.getAnswers().stream().anyMatch(a -> a.getQuestion().hasCode())
                && loaded.getAnswers().stream().allMatch(a -> a.getQuestion().getSubtopicName() != null));

        QuizResult tr = svc.submit(topic, true);
        check("history: subtopic title", tr.getTitle().equals("Data Structures › Linked Lists") && tr.isAutoSubmitted());

        List<TopicStat> stats = svc.subtopicStats(student);
        int attempted = stats.stream().mapToInt(TopicStat::getAttempted).sum();
        check("analytics: per-subtopic stats cover every answered question (skipped ones excluded)",
                attempted == r.getCorrect() + r.getWrong() + tr.getCorrect() + tr.getWrong());
        check("analytics: weakest subtopic listed first", stats.size() < 2 || stats.get(0).getAccuracy() <= stats.get(stats.size() - 1).getAccuracy());
        User admin = new AuthService(dao.users()).login("admin", "admin123");
        check("analytics: admin sees class-wide data", svc.historyFor(admin).size() == 2);

        Subtopic empty = dao.subtopics().save(new Subtopic(0, ds.getId(), "Empty One"));
        expect("quiz: empty subtopic cannot start", ValidationException.class, () -> svc.startQuiz(student, ds, empty, Difficulty.HARD));
        dao.subtopics().delete(empty.getId());
    }

    private interface QPred { boolean test(Question q); }

    private static boolean allMatch(QuizSession s, QPred p) {
        for (int i = 0; i < s.size(); i++) if (!p.test(s.getQuestion(i))) return false;
        return true;
    }

    private static long distinct(QuizSession s) {
        return java.util.stream.IntStream.range(0, s.size()).map(i -> s.getQuestion(i).getId()).distinct().count();
    }

    private static long countSubtopics(QuizSession s) {
        return java.util.stream.IntStream.range(0, s.size()).map(i -> s.getQuestion(i).getSubtopicId()).distinct().count();
    }

    // ------------------------------------------------------------------ admin

    private static void testValidation() {
        Question q = new Question(0, 1, Difficulty.EASY, "What?", new String[]{"a", "b", "c", "d"}, 2);
        check("validate: valid question passes", noThrow(() -> AdminService.validate(q)));
        expect("validate: empty option", ValidationException.class, () -> AdminService.validate(
                new Question(0, 1, Difficulty.EASY, "What?", new String[]{"a", "", "c", "d"}, 0)));
        expect("validate: duplicate option", ValidationException.class, () -> AdminService.validate(
                new Question(0, 1, Difficulty.EASY, "What?", new String[]{"a", " a ", "c", "d"}, 0)));
        expect("validate: no correct answer", ValidationException.class, () -> AdminService.validate(
                new Question(0, 1, Difficulty.EASY, "What?", new String[]{"a", "b", "c", "d"}, -1)));
        expect("validate: missing subtopic", ValidationException.class, () -> AdminService.validate(
                new Question(0, 0, Difficulty.EASY, "What?", new String[]{"a", "b", "c", "d"}, 1)));
    }

    private static void testCrudAndCascade(DAOFactory dao) throws Exception {
        AdminService admin = new AdminService(dao);
        Subject s = admin.saveSubject(new Subject(0, "  Compiler Design ", "cd", "Lexing and parsing", "#123ABC"));
        check("admin: subject saved and trimmed", s.getId() > 0 && s.getName().equals("Compiler Design") && s.getShortCode().equals("CD"));
        expect("admin: duplicate subject rejected", DataAccessException.class,
                () -> admin.saveSubject(new Subject(0, "compiler design", "X", "", "#000000")));
        expect("admin: bad colour rejected", ValidationException.class,
                () -> admin.saveSubject(new Subject(0, "Y", "Y", "", "red")));
        Subtopic t = admin.saveSubtopic(new Subtopic(0, s.getId(), "Lexical Analysis"));
        expect("admin: duplicate subtopic rejected", DataAccessException.class,
                () -> admin.saveSubtopic(new Subtopic(0, s.getId(), "lexical analysis")));
        Question q = new Question(0, t.getId(), Difficulty.HARD, "Tokens?", new String[]{"a", "b", "c", "d"}, 3);
        q.setCode("int x = 1;");
        q.setCodeLang("c");
        q.setHint("think");
        admin.saveQuestion(q);
        Question back = dao.questions().findById(q.getId()).orElseThrow(IllegalStateException::new);
        check("admin: question with code/hint round-trips", "int x = 1;".equals(back.getCode()) && "c".equals(back.getCodeLang())
                && "think".equals(back.getHint()) && back.getSubjectName().equals("Compiler Design"));
        Subject reloaded = dao.subjects().findById(s.getId()).orElseThrow(IllegalStateException::new);
        check("admin: subject counts per difficulty", reloaded.getQuestionCount(Difficulty.HARD) == 1 && reloaded.getSubtopicCount() == 1);

        QuizService svc = new QuizService(dao);
        User student = new AuthService(dao.users()).login("student", "student123");
        QuizSession sess = svc.startQuiz(student, reloaded, null, Difficulty.HARD);
        sess.selectAnswer(3);
        QuizResult r = svc.submit(sess, false);
        admin.deleteSubject(reloaded);
        check("admin: subject delete cascades to subtopics and questions",
                !dao.questions().findById(q.getId()).isPresent() && !dao.subtopics().findById(t.getId()).isPresent());
        QuizResult kept = svc.historyFor(student).stream().filter(x -> x.getId() == r.getId()).findFirst().orElse(null);
        check("admin: results survive subject deletion", kept != null && kept.getSubjectName().equals("Compiler Design"));
        svc.loadAnswers(kept);
        check("admin: review survives subject deletion", kept.getAnswers().size() == 1 && kept.getAnswers().get(0).isCorrect()
                && "int x = 1;".equals(kept.getAnswers().get(0).getQuestion().getCode()));
    }

    private static void testImportExport(DAOFactory dao) throws Exception {
        AdminService admin = new AdminService(dao);
        AdminService.ImportReport rep = admin.importBank(new BufferedReader(new StringReader(SAMPLE_BANK)), "sample.qb");
        check("import: 2 imported, 2 errors", rep.imported == 2 && rep.errors.size() == 2);
        check("import: subject and subtopics created", rep.subjectsCreated == 1 && rep.subtopicsCreated == 2);
        Subject s = dao.subjects().findByName("Sample Subject").orElseThrow(IllegalStateException::new);
        check("import: colour and badge kept", s.getColor().equals("#123456") && s.getShortCode().equals("SS"));

        Path out = Files.createTempFile("export", ".qb");
        int n = admin.exportBank(out, s);
        check("export: count", n == 2);
        admin.deleteSubject(s);
        AdminService.ImportReport again = admin.importBank(out);
        check("export -> import round trip", again.imported == 2 && again.errors.isEmpty());
        Subject s2 = dao.subjects().findByName("Sample Subject").orElseThrow(IllegalStateException::new);
        Question code = dao.questions().find(s2.getId(), 0, Difficulty.MEDIUM).get(0);
        check("round trip keeps code and hint", code.getCode().contains("System.out.println(1 + 1);") && code.hasHint());
        admin.deleteSubject(s2);

        Path res = Files.createTempFile("results", ".csv");
        AdminService.exportResults(res, dao.results().findAll());
        check("results CSV: header + rows", Files.readAllLines(res).size() == 1 + dao.results().findAll().size());
        Files.deleteIfExists(out);
        Files.deleteIfExists(res);
    }

    // ------------------------------------------------------------------ mini framework

    private interface Body { void run() throws Exception; }

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("  PASS  " + name);
        } else {
            failed++;
            System.out.println("  FAIL  " + name);
        }
    }

    private static boolean noThrow(Body b) {
        try {
            b.run();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void expect(String name, Class<? extends Throwable> type, Body b) {
        try {
            b.run();
            check(name + " (no exception thrown)", false);
        } catch (Throwable t) {
            check(name, type.isInstance(t));
        }
    }
}
