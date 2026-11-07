package com.quiz.ui;

import com.quiz.model.Difficulty;
import com.quiz.model.QuizResult;
import com.quiz.model.User;
import com.quiz.service.AdminService;
import com.quiz.ui.components.PillButton;
import com.quiz.ui.components.RoundPanel;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Table of past attempts (own attempts for students, everyone's for admins) with filters, review and CSV export. */
public class HistoryPanel extends JPanel {

    private final List<QuizResult> all = new ArrayList<>();
    private final List<QuizResult> shown = new ArrayList<>();
    private final MainFrame frame;
    private final boolean admin;
    private final ResultModel model;   // created in the constructor: its columns depend on 'admin'
    private final JTable table;
    private final javax.swing.JLabel summary = Theme.label(" ", Theme.BODY_BOLD, Theme.TEXT);
    private final JComboBox<String> subjectFilter = new JComboBox<>();
    private final JComboBox<Object> modeFilter = new JComboBox<>();

    public HistoryPanel(AppContext ctx, MainFrame frame) {
        super(new BorderLayout());
        this.frame = frame;
        User user = ctx.getCurrentUser();
        this.admin = user.isAdmin();
        this.model = new ResultModel();
        this.table = new JTable(model);
        setBackground(Theme.BG);

        AppBar bar = new AppBar(admin ? "All Results" : "My Results",
                admin ? "Every attempt by every student" : "Every test you have taken");
        PillButton back = new PillButton("‹ Dashboard", PillButton.Style.GHOST);
        PillButton analytics = new PillButton("Analytics", PillButton.Style.GHOST);
        bar.action(analytics).action(back);
        add(bar, BorderLayout.NORTH);

        UiUtil.run(this, () -> all.addAll(ctx.quiz().historyFor(user)));

        subjectFilter.addItem("All subjects");
        all.stream().map(QuizResult::getSubjectName).distinct().sorted().forEach(subjectFilter::addItem);
        modeFilter.addItem("All modes");
        for (Difficulty d : Difficulty.values()) modeFilter.addItem(d);
        subjectFilter.setPrototypeDisplayValue("Database Management Systems  ");
        modeFilter.setPrototypeDisplayValue("All modes    ");

        Theme.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        String[] cols = model.cols;
        for (int c = 0; c < cols.length; c++) {
            int w = cols[c].equals("Date") ? 140 : cols[c].equals("Test") ? (admin ? 200 : 250) : cols[c].equals("Student") ? 130
                    : cols[c].equals("Mode") || cols[c].equals("Score") ? 75 : 68;
            table.getColumnModel().getColumn(c).setPreferredWidth(w);
            // never squeeze the date or mode into "2026-11-07 1..." / "Medi..."
            if (cols[c].equals("Date")) table.getColumnModel().getColumn(c).setMinWidth(138);
            if (cols[c].equals("Mode")) table.getColumnModel().getColumn(c).setMinWidth(70);
            if (w == 68) table.getColumnModel().getColumn(c).setMinWidth(64);   // "Percent", "Correct", "Skipped" headers stay readable
        }
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) reviewSelected();
            }
        });

        RoundPanel card = new RoundPanel(new BorderLayout(0, 12), Theme.CARD);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(summary, BorderLayout.WEST);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filters.setOpaque(false);
        filters.add(subjectFilter);
        filters.add(modeFilter);
        top.add(filters, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(javax.swing.BorderFactory.createLineBorder(Theme.BORDER));
        card.add(sp, BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.setBorder(Theme.pad(18, 24));
        center.add(card);
        add(center, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 24, 18, 24));
        PillButton export = new PillButton("Export to CSV", PillButton.Style.SECONDARY);
        PillButton review = new PillButton("Review Selected", PillButton.Style.WARM);
        actions.add(export);
        actions.add(review);
        add(actions, BorderLayout.SOUTH);

        back.addActionListener(e -> frame.showDashboard());
        analytics.addActionListener(e -> frame.showAnalytics());
        review.addActionListener(e -> reviewSelected());
        export.addActionListener(e -> exportCsv());
        subjectFilter.addActionListener(e -> applyFilter());
        modeFilter.addActionListener(e -> applyFilter());
        applyFilter();
    }

    private void applyFilter() {
        Object subj = subjectFilter.getSelectedItem();
        Object mode = modeFilter.getSelectedItem();
        shown.clear();
        shown.addAll(all.stream()
                .filter(r -> subjectFilter.getSelectedIndex() <= 0 || r.getSubjectName().equals(subj))
                .filter(r -> !(mode instanceof Difficulty) || r.getDifficulty() == mode)
                .collect(Collectors.toList()));
        model.fireTableDataChanged();
        double avg = shown.stream().mapToDouble(QuizResult::getPercentage).average().orElse(0);
        double best = shown.stream().mapToDouble(QuizResult::getPercentage).max().orElse(0);
        long passed = shown.stream().filter(QuizResult::isPassed).count();
        summary.setText(shown.isEmpty() ? "No attempts to show." : String.format(
                "%d attempt(s)   •   average %.1f%%   •   best %.1f%%   •   passed %d",
                shown.size(), avg, best, passed));
    }

    private void reviewSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UiUtil.warn(this, "Select an attempt to review.");
            return;
        }
        frame.showReview(shown.get(table.convertRowIndexToModel(row)), true);
    }

    private void exportCsv() {
        if (shown.isEmpty()) {
            UiUtil.warn(this, "There are no results to export.");
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));
        fc.setSelectedFile(new File("quiz_results.csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        File target = f.getName().toLowerCase().endsWith(".csv") ? f : new File(f.getPath() + ".csv");
        if (target.exists() && !UiUtil.confirm(this, target.getName() + " exists. Overwrite?", "Export")) return;
        UiUtil.run(this, () -> {
            AdminService.exportResults(target.toPath(), shown);
            UiUtil.info(this, "Exported " + shown.size() + " result(s) to\n" + target.getAbsolutePath());
        });
    }

    private class ResultModel extends AbstractTableModel {
        final String[] cols = admin
                ? new String[]{"Date", "Student", "Test", "Mode", "Score", "Percent", "Grade", "Correct", "Wrong", "Skipped", "Hints", "Time"}
                : new String[]{"Date", "Test", "Mode", "Score", "Percent", "Grade", "Correct", "Wrong", "Skipped", "Hints", "Time"};
        private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        @Override public int getRowCount() { return shown.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }

        @Override
        public Class<?> getColumnClass(int c) {
            switch (cols[c]) {
                case "Correct": case "Wrong": case "Skipped": case "Hints": return Integer.class;
                case "Percent": return Double.class;
                default: return String.class;
            }
        }

        @Override
        public Object getValueAt(int row, int c) {
            QuizResult r = shown.get(row);
            switch (cols[c]) {
                case "Date": return r.getTakenAt().format(fmt);
                case "Student": return r.getUserName();
                case "Test": return r.getTitle();
                case "Mode": return r.getDifficulty().getLabel();
                case "Score": return AdminService.fmt(r.getScore()) + " / " + AdminService.fmt(r.getMaxScore());
                case "Percent": return Math.round(r.getPercentage() * 10) / 10.0;
                case "Grade": return r.getGrade();
                case "Correct": return r.getCorrect();
                case "Wrong": return r.getWrong();
                case "Skipped": return r.getUnanswered();
                case "Hints": return r.getHintsUsed();
                default: return QuizResult.formatDuration(r.getTimeTakenSeconds());
            }
        }
    }
}
