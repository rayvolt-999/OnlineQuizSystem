package com.quiz.ui;

import com.quiz.model.Difficulty;
import com.quiz.model.Question;
import com.quiz.model.Subject;
import com.quiz.model.Subtopic;
import com.quiz.model.User;
import com.quiz.service.AdminService;
import com.quiz.ui.components.PillButton;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Admin-only screen: question bank (with filters and .qb import/export), subjects & subtopics, and accounts. */
public class AdminPanel extends JPanel {

    private static final String ALL = "All";

    private final AdminService admin;
    private final List<Subject> subjects = new ArrayList<>();
    private final List<Question> questions = new ArrayList<>();
    private final List<Subtopic> topicsOfSelected = new ArrayList<>();
    private final List<User> users = new ArrayList<>();

    private final JComboBox<Object> subjectFilter = new JComboBox<>();
    private final JComboBox<Object> subtopicFilter = new JComboBox<>();
    private final JComboBox<Object> modeFilter = new JComboBox<>();
    private final JLabel countLabel = Theme.label(" ", Theme.SMALL_BOLD, Theme.MUTED);

    private final QuestionModel questionModel = new QuestionModel();
    private final SubjectModel subjectModel = new SubjectModel();
    private final SubtopicModel subtopicModel = new SubtopicModel();
    private final UserModel userModel = new UserModel();
    private final JTable questionTable = new JTable(questionModel);
    private final JTable subjectTable = new JTable(subjectModel);
    private final JTable subtopicTable = new JTable(subtopicModel);
    private final JTable userTable = new JTable(userModel);

    public AdminPanel(AppContext ctx, MainFrame frame) {
        super(new BorderLayout());
        this.admin = ctx.admin();
        setBackground(Theme.BG);

        AppBar bar = new AppBar("Admin Panel", "Manage subjects, subtopics, the question bank and accounts");
        PillButton back = new PillButton("‹ Dashboard", PillButton.Style.GHOST);
        back.addActionListener(e -> frame.showDashboard());
        bar.action(back);
        add(bar, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.BODY_BOLD);
        tabs.addTab("Question Bank", questionsTab());
        tabs.addTab("Subjects & Subtopics", subjectsTab());
        tabs.addTab("Users", usersTab());
        tabs.addChangeListener(e -> reloadAll());
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(Theme.pad(16, 22));
        wrap.add(tabs);
        add(wrap, BorderLayout.CENTER);

        modeFilter.addItem(ALL + " modes");
        for (Difficulty d : Difficulty.values()) modeFilter.addItem(d);
        subjectFilter.setPrototypeDisplayValue("Database Management Systems  ");
        subtopicFilter.setPrototypeDisplayValue("GUI (AWT/Swing) & JDBC     ");
        modeFilter.setPrototypeDisplayValue("All modes    ");
        reloadAll();
    }

    // ===================================================================== question bank

    private JPanel questionsTab() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBorder(Theme.pad(12, 12));
        JPanel top = new JPanel(new BorderLayout());
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.add(Theme.label("Filter:", Theme.SMALL_BOLD, Theme.TEXT));
        filters.add(subjectFilter);
        filters.add(subtopicFilter);
        filters.add(modeFilter);
        top.add(filters, BorderLayout.WEST);
        top.add(countLabel, BorderLayout.EAST);
        p.add(top, BorderLayout.NORTH);

        subjectFilter.addActionListener(e -> { fillSubtopicFilter(); reloadQuestions(); });
        subtopicFilter.addActionListener(e -> reloadQuestions());
        modeFilter.addActionListener(e -> reloadQuestions());

        Theme.styleTable(questionTable);
        questionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int[] widths = {45, 150, 170, 85, 430, 55, 180};
        for (int i = 0; i < widths.length; i++) questionTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        questionTable.addMouseListener(doubleClick(this::editQuestion));
        p.add(new JScrollPane(questionTable), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        PillButton imp = new PillButton("Import .qb", PillButton.Style.SECONDARY);
        PillButton exp = new PillButton("Export .qb", PillButton.Style.SECONDARY);
        PillButton del = new PillButton("Delete", PillButton.Style.DANGER);
        PillButton edit = new PillButton("Edit", PillButton.Style.PRIMARY);
        PillButton add = new PillButton("Add Question", PillButton.Style.WARM);
        actions.add(imp);
        actions.add(exp);
        actions.add(del);
        actions.add(edit);
        actions.add(add);
        p.add(actions, BorderLayout.SOUTH);

        add.addActionListener(e -> addQuestion());
        edit.addActionListener(e -> editQuestion());
        del.addActionListener(e -> deleteQuestion());
        imp.addActionListener(e -> importBank());
        exp.addActionListener(e -> exportBank());
        return p;
    }

    private Subject filterSubject() {
        Object o = subjectFilter.getSelectedItem();
        return o instanceof Subject ? (Subject) o : null;
    }

    private Subtopic filterSubtopic() {
        Object o = subtopicFilter.getSelectedItem();
        return o instanceof Subtopic ? (Subtopic) o : null;
    }

    private Difficulty filterMode() {
        Object o = modeFilter.getSelectedItem();
        return o instanceof Difficulty ? (Difficulty) o : null;
    }

    private void fillSubtopicFilter() {
        withoutEvents(subtopicFilter, () -> {
            subtopicFilter.removeAllItems();
            subtopicFilter.addItem(ALL + " subtopics");
            Subject s = filterSubject();
            if (s != null) {
                List<Subtopic> list = new ArrayList<>();
                UiUtil.run(this, () -> list.addAll(admin.getSubtopics(s)));
                for (Subtopic t : list) subtopicFilter.addItem(t);
            }
        });
    }

    private void addQuestion() {
        if (subjects.isEmpty()) {
            UiUtil.warn(this, "Create a subject and a subtopic first (Subjects & Subtopics tab).");
            return;
        }
        QuestionDialog d = new QuestionDialog(owner(), admin, subjects, null, filterSubject(), filterSubtopic());
        d.setVisible(true);
        if (d.isSaved()) reloadAll();
    }

    private Question selectedQuestion() {
        int row = questionTable.getSelectedRow();
        if (row < 0) {
            UiUtil.warn(this, "Select a question first.");
            return null;
        }
        return questions.get(questionTable.convertRowIndexToModel(row));
    }

    private void editQuestion() {
        Question q = selectedQuestion();
        if (q == null) return;
        QuestionDialog d = new QuestionDialog(owner(), admin, subjects, q.copy(), null, null);
        d.setVisible(true);
        if (d.isSaved()) reloadAll();
    }

    private void deleteQuestion() {
        Question q = selectedQuestion();
        if (q == null) return;
        if (!UiUtil.confirm(this, UiUtil.wrap("Delete this question?\n\n" + q.getText(), 380), "Delete Question")) return;
        UiUtil.run(this, () -> admin.deleteQuestion(q));
        reloadAll();
    }

    private void importBank() {
        JFileChooser fc = new JFileChooser(new File("."));
        fc.setFileFilter(new FileNameExtensionFilter("Question bank (*.qb, *.txt)", "qb", "txt"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        UiUtil.run(this, () -> {
            AdminService.ImportReport rep = admin.importBank(fc.getSelectedFile().toPath());
            StringBuilder sb = new StringBuilder("Imported " + rep.imported + " question(s)");
            if (rep.subjectsCreated > 0) sb.append(", ").append(rep.subjectsCreated).append(" new subject(s)");
            if (rep.subtopicsCreated > 0) sb.append(", ").append(rep.subtopicsCreated).append(" new subtopic(s)");
            sb.append('.');
            if (!rep.errors.isEmpty()) {
                sb.append("\n\nSkipped ").append(rep.errors.size()).append(" problem(s):\n");
                rep.errors.stream().limit(15).forEach(err -> sb.append("  • ").append(err).append('\n'));
                if (rep.errors.size() > 15) sb.append("  …");
            }
            UiUtil.info(this, sb.toString());
        });
        reloadAll();
    }

    private void exportBank() {
        JFileChooser fc = new JFileChooser(new File("."));
        fc.setFileFilter(new FileNameExtensionFilter("Question bank (*.qb)", "qb"));
        Subject s = filterSubject();
        fc.setSelectedFile(new File(s == null ? "question_bank.qb" : s.getName().replaceAll("\\W+", "_").toLowerCase() + ".qb"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        File target = f.getName().contains(".") ? f : new File(f.getPath() + ".qb");
        if (target.exists() && !UiUtil.confirm(this, target.getName() + " exists. Overwrite?", "Export")) return;
        UiUtil.run(this, () -> {
            int n = admin.exportBank(target.toPath(), s);
            UiUtil.info(this, "Exported " + n + " question(s) to\n" + target.getAbsolutePath());
        });
    }

    // ===================================================================== subjects & subtopics

    private JPanel subjectsTab() {
        JPanel p = new JPanel(new GridLayout(1, 2, 16, 0));
        p.setBorder(Theme.pad(12, 12));

        Theme.styleTable(subjectTable);
        subjectTable.setAutoCreateRowSorter(false);
        subjectTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        subjectTable.getColumnModel().getColumn(0).setPreferredWidth(230);
        subjectTable.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) reloadSubtopics(); });
        subjectTable.addMouseListener(doubleClick(this::editSubject));
        JPanel left = new JPanel(new BorderLayout(0, 8));
        left.add(Theme.label("Subjects", Theme.HEADING, Theme.TEXT), BorderLayout.NORTH);
        left.add(new JScrollPane(subjectTable), BorderLayout.CENTER);
        JPanel la = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        PillButton sDel = new PillButton("Delete", PillButton.Style.DANGER);
        PillButton sEdit = new PillButton("Edit", PillButton.Style.PRIMARY);
        PillButton sAdd = new PillButton("Add Subject", PillButton.Style.WARM);
        la.add(sDel);
        la.add(sEdit);
        la.add(sAdd);
        left.add(la, BorderLayout.SOUTH);

        Theme.styleTable(subtopicTable);
        subtopicTable.setAutoCreateRowSorter(false);
        subtopicTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        subtopicTable.getColumnModel().getColumn(0).setPreferredWidth(220);
        subtopicTable.addMouseListener(doubleClick(this::renameSubtopic));
        JPanel right = new JPanel(new BorderLayout(0, 8));
        right.add(Theme.label("Subtopics of the selected subject", Theme.HEADING, Theme.TEXT), BorderLayout.NORTH);
        right.add(new JScrollPane(subtopicTable), BorderLayout.CENTER);
        JPanel ra = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        PillButton tDel = new PillButton("Delete", PillButton.Style.DANGER);
        PillButton tRen = new PillButton("Rename", PillButton.Style.PRIMARY);
        PillButton tAdd = new PillButton("Add Subtopic", PillButton.Style.WARM);
        ra.add(tDel);
        ra.add(tRen);
        ra.add(tAdd);
        right.add(ra, BorderLayout.SOUTH);

        p.add(left);
        p.add(right);

        sAdd.addActionListener(e -> {
            SubjectDialog d = new SubjectDialog(owner(), admin, null);
            d.setVisible(true);
            if (d.isSaved()) reloadAll();
        });
        sEdit.addActionListener(e -> editSubject());
        sDel.addActionListener(e -> {
            Subject s = selectedSubject();
            if (s == null) return;
            if (!UiUtil.confirm(this, "Delete subject '" + s.getName() + "' with its " + s.getSubtopicCount()
                    + " subtopic(s) and " + s.getTotalQuestions() + " question(s)?\nPast results are kept.", "Delete Subject")) return;
            UiUtil.run(this, () -> admin.deleteSubject(s));
            reloadAll();
        });
        tAdd.addActionListener(e -> {
            Subject s = selectedSubject();
            if (s == null) return;
            String name = JOptionPane.showInputDialog(this, "New subtopic name for " + s.getName() + ":", "Add Subtopic",
                    JOptionPane.PLAIN_MESSAGE);
            if (name == null) return;
            UiUtil.run(this, () -> admin.saveSubtopic(new Subtopic(0, s.getId(), name)));
            reloadAll();
        });
        tRen.addActionListener(e -> renameSubtopic());
        tDel.addActionListener(e -> {
            Subtopic t = selectedSubtopic();
            if (t == null) return;
            if (!UiUtil.confirm(this, "Delete subtopic '" + t.getName() + "' and its " + t.getTotalQuestions()
                    + " question(s)?", "Delete Subtopic")) return;
            UiUtil.run(this, () -> admin.deleteSubtopic(t));
            reloadAll();
        });
        return p;
    }

    private Subject selectedSubject() {
        int row = subjectTable.getSelectedRow();
        if (row < 0) {
            UiUtil.warn(this, "Select a subject first.");
            return null;
        }
        return subjects.get(row);
    }

    private Subtopic selectedSubtopic() {
        int row = subtopicTable.getSelectedRow();
        if (row < 0) {
            UiUtil.warn(this, "Select a subtopic first.");
            return null;
        }
        return topicsOfSelected.get(row);
    }

    private void editSubject() {
        Subject s = selectedSubject();
        if (s == null) return;
        SubjectDialog d = new SubjectDialog(owner(), admin, s);
        d.setVisible(true);
        if (d.isSaved()) reloadAll();
    }

    private void renameSubtopic() {
        Subtopic t = selectedSubtopic();
        if (t == null) return;
        Object name = JOptionPane.showInputDialog(this, "Rename subtopic:", "Rename", JOptionPane.PLAIN_MESSAGE,
                null, null, t.getName());
        if (name == null) return;
        UiUtil.run(this, () -> {
            Subtopic copy = new Subtopic(t.getId(), t.getSubjectId(), name.toString());
            admin.saveSubtopic(copy);
        });
        reloadAll();
    }

    private void reloadSubtopics() {
        topicsOfSelected.clear();
        int row = subjectTable.getSelectedRow();
        if (row >= 0) {
            Subject s = subjects.get(row);
            UiUtil.run(this, () -> topicsOfSelected.addAll(admin.getSubtopics(s)));
        }
        subtopicModel.fireTableDataChanged();
    }

    // ===================================================================== users

    private JPanel usersTab() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBorder(Theme.pad(12, 12));
        Theme.styleTable(userTable);
        p.add(new JScrollPane(userTable), BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        PillButton add = new PillButton("Add Admin", PillButton.Style.WARM);
        actions.add(Theme.label("Students create their own accounts from the login screen.", Theme.SMALL, Theme.MUTED));
        actions.add(add);
        p.add(actions, BorderLayout.SOUTH);
        add.addActionListener(e -> addAdmin());
        return p;
    }

    private void addAdmin() {
        JTextField name = new JTextField(18);
        JTextField user = new JTextField(18);
        JPasswordField pass = new JPasswordField(18);
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(Theme.label("Full name", Theme.BODY, Theme.TEXT));
        form.add(name);
        form.add(Theme.label("Username", Theme.BODY, Theme.TEXT));
        form.add(user);
        form.add(Theme.label("Password", Theme.BODY, Theme.TEXT));
        form.add(pass);
        int ok = JOptionPane.showConfirmDialog(this, form, "Add Admin", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;
        UiUtil.run(this, () -> {
            User u = admin.createAdmin(user.getText(), name.getText(), new String(pass.getPassword()));
            UiUtil.info(this, "Admin '" + u.getUsername() + "' created.");
        });
        reloadAll();
    }

    // ===================================================================== loading

    private void reloadAll() {
        Object keepSubject = subjectFilter.getSelectedItem();
        Object keepTopic = subtopicFilter.getSelectedItem();
        int keepSubjectRow = subjectTable.getSelectedRow();
        UiUtil.run(this, () -> {
            subjects.clear();
            subjects.addAll(admin.getSubjects());
            users.clear();
            users.addAll(admin.getUsers());
        });
        withoutEvents(subjectFilter, () -> {
            subjectFilter.removeAllItems();
            subjectFilter.addItem(ALL + " subjects");
            for (Subject s : subjects) {
                subjectFilter.addItem(s);
                if (keepSubject instanceof Subject && ((Subject) keepSubject).getId() == s.getId()) subjectFilter.setSelectedItem(s);
            }
        });
        fillSubtopicFilter();
        if (keepTopic instanceof Subtopic) {
            for (int i = 0; i < subtopicFilter.getItemCount(); i++) {
                Object o = subtopicFilter.getItemAt(i);
                if (o instanceof Subtopic && ((Subtopic) o).getId() == ((Subtopic) keepTopic).getId()) {
                    final int idx = i;
                    withoutEvents(subtopicFilter, () -> subtopicFilter.setSelectedIndex(idx));
                }
            }
        }
        subjectModel.fireTableDataChanged();
        if (keepSubjectRow >= 0 && keepSubjectRow < subjects.size()) subjectTable.setRowSelectionInterval(keepSubjectRow, keepSubjectRow);
        reloadSubtopics();
        userModel.fireTableDataChanged();
        reloadQuestions();
    }

    private void reloadQuestions() {
        UiUtil.run(this, () -> {
            questions.clear();
            questions.addAll(admin.getQuestions(filterSubject(), filterSubtopic(), filterMode()));
        });
        questionModel.fireTableDataChanged();
        countLabel.setText(questions.size() + " question(s)");
    }

    private Window owner() {
        return SwingUtilities.getWindowAncestor(this);
    }

    private static MouseAdapter doubleClick(Runnable r) {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) r.run();
            }
        };
    }

    /** Temporarily removes a combo box's listeners while its items are rebuilt. */
    private static void withoutEvents(JComboBox<?> box, Runnable r) {
        ActionListener[] ls = box.getActionListeners();
        for (ActionListener l : ls) box.removeActionListener(l);
        try {
            r.run();
        } finally {
            for (ActionListener l : ls) box.addActionListener(l);
        }
    }

    // ===================================================================== table models

    private class QuestionModel extends AbstractTableModel {
        private final String[] cols = {"ID", "Subject", "Subtopic", "Mode", "Question", "Code", "Answer"};
        @Override public int getRowCount() { return questions.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Class<?> getColumnClass(int c) { return c == 0 ? Integer.class : String.class; }

        @Override
        public Object getValueAt(int r, int c) {
            Question q = questions.get(r);
            switch (c) {
                case 0: return q.getId();
                case 1: return q.getSubjectName();
                case 2: return q.getSubtopicName();
                case 3: return q.getDifficulty().getLabel();
                case 4: return q.getText().replace('\n', ' ');
                case 5: return q.hasCode() ? (q.getCodeLang() == null ? "yes" : q.getCodeLang()) : "";
                default: return Question.label(q.getCorrectIndex()) + ". " + q.getOption(q.getCorrectIndex());
            }
        }
    }

    private class SubjectModel extends AbstractTableModel {
        private final String[] cols = {"Subject", "Badge", "Subtopics", "Easy", "Medium", "Hard"};
        @Override public int getRowCount() { return subjects.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Class<?> getColumnClass(int c) { return c >= 2 ? Integer.class : String.class; }

        @Override
        public Object getValueAt(int r, int c) {
            Subject s = subjects.get(r);
            switch (c) {
                case 0: return s.getName();
                case 1: return s.getShortCode();
                case 2: return s.getSubtopicCount();
                case 3: return s.getQuestionCount(Difficulty.EASY);
                case 4: return s.getQuestionCount(Difficulty.MEDIUM);
                default: return s.getQuestionCount(Difficulty.HARD);
            }
        }
    }

    private class SubtopicModel extends AbstractTableModel {
        private final String[] cols = {"Subtopic", "Easy", "Medium", "Hard"};
        @Override public int getRowCount() { return topicsOfSelected.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Class<?> getColumnClass(int c) { return c >= 1 ? Integer.class : String.class; }

        @Override
        public Object getValueAt(int r, int c) {
            Subtopic t = topicsOfSelected.get(r);
            switch (c) {
                case 0: return t.getName();
                case 1: return t.getQuestionCount(Difficulty.EASY);
                case 2: return t.getQuestionCount(Difficulty.MEDIUM);
                default: return t.getQuestionCount(Difficulty.HARD);
            }
        }
    }

    private class UserModel extends AbstractTableModel {
        private final String[] cols = {"Username", "Full name", "Role"};
        @Override public int getRowCount() { return users.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }

        @Override
        public Object getValueAt(int r, int c) {
            User u = users.get(r);
            switch (c) {
                case 0: return u.getUsername();
                case 1: return u.getFullName();
                default: return u.getRole().name();
            }
        }
    }
}
