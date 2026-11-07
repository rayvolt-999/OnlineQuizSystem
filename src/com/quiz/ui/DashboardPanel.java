package com.quiz.ui;

import com.quiz.model.Difficulty;
import com.quiz.model.QuizResult;
import com.quiz.model.Subject;
import com.quiz.model.User;
import com.quiz.ui.components.Badge;
import com.quiz.ui.components.Chip;
import com.quiz.ui.components.PillButton;
import com.quiz.ui.components.RoundPanel;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/** Home screen: greeting, progress stats, a grid of colourful subject cards and recent activity. */
public class DashboardPanel extends JPanel {

    public DashboardPanel(AppContext ctx, MainFrame frame) {
        super(new BorderLayout());
        setBackground(Theme.BG);
        User user = ctx.getCurrentUser();

        List<Subject> subjects = new ArrayList<>();
        List<QuizResult> history = new ArrayList<>();
        UiUtil.run(this, () -> {
            subjects.addAll(ctx.quiz().getSubjects());
            history.addAll(ctx.quiz().historyFor(user));
        });

        // ---------------- app bar ----------------
        AppBar bar = new AppBar("Hi, " + user.getFullName().split(" ")[0] + "!",
                user.isAdmin() ? "Administrator • manage the question bank or take a test yourself"
                        : "Pick a subject, choose a mode and start practising");
        PillButton analytics = new PillButton("Analytics", PillButton.Style.GHOST);
        PillButton results = new PillButton(user.isAdmin() ? "All Results" : "My Results", PillButton.Style.GHOST);
        PillButton logout = new PillButton("Logout", PillButton.Style.GHOST);
        bar.action(analytics).action(results);
        if (user.isAdmin()) {
            PillButton admin = new PillButton("Admin Panel", PillButton.Style.WARM);
            admin.addActionListener(e -> frame.showAdmin());
            bar.action(admin);
        }
        bar.action(logout);
        add(bar, BorderLayout.NORTH);

        // ---------------- body ----------------
        JPanel body = UiUtil.widthTracking(null);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(Theme.pad(22, 26));

        double avg = history.stream().mapToDouble(QuizResult::getPercentage).average().orElse(0);
        double best = history.stream().mapToDouble(QuizResult::getPercentage).max().orElse(0);
        int questions = subjects.stream().mapToInt(Subject::getTotalQuestions).sum();
        JPanel stats = new JPanel(new GridLayout(1, 4, 16, 0));
        stats.setOpaque(false);
        stats.add(statCard(user.isAdmin() ? "Tests taken (all)" : "Tests taken", String.valueOf(history.size()), Theme.INDIGO));
        stats.add(statCard("Average score", history.isEmpty() ? "–" : String.format("%.0f%%", avg), Theme.TEAL));
        stats.add(statCard("Best score", history.isEmpty() ? "–" : String.format("%.0f%%", best), Theme.ORANGE));
        stats.add(statCard("Question bank", questions + " Qs", Theme.ROSE));
        body.add(UiUtil.capHeight(stats));
        body.add(Box.createVerticalStrut(26));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(Theme.label("Subjects", Theme.TITLE, Theme.TEXT), BorderLayout.WEST);
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        legend.setOpaque(false);
        for (Difficulty d : Difficulty.values()) {
            legend.add(Chip.soft(d.getLabel() + ": " + d.getQuestionCount() + " Qs • " + d.getMinutes() + " min", Theme.color(d)));
        }
        head.add(legend, BorderLayout.EAST);
        body.add(UiUtil.capHeight(head));
        body.add(Box.createVerticalStrut(14));

        JPanel grid = new JPanel(new GridLayout(0, 4, 16, 16));
        grid.setOpaque(false);
        for (Subject s : subjects) grid.add(subjectCard(s, frame));
        if (subjects.isEmpty()) {
            grid.add(Theme.label("No subjects yet. An admin can add them in the Admin Panel.", Theme.BODY, Theme.MUTED));
        }
        body.add(UiUtil.capHeight(grid));

        if (!history.isEmpty()) {
            body.add(Box.createVerticalStrut(26));
            body.add(UiUtil.capHeight(Theme.label("Recent activity", Theme.HEADING, Theme.TEXT)));
            body.add(Box.createVerticalStrut(10));
            for (QuizResult r : history.subList(0, Math.min(3, history.size()))) {
                body.add(UiUtil.capHeight(recentRow(r, user.isAdmin(), frame)));
                body.add(Box.createVerticalStrut(8));
            }
        }
        add(UiUtil.scroll(body), BorderLayout.CENTER);
        UiUtil.relayoutLater(body);

        analytics.addActionListener(e -> frame.showAnalytics());
        results.addActionListener(e -> frame.showHistory());
        logout.addActionListener(e -> frame.showLogin());
    }

    private static JComponent statCard(String title, String value, Color color) {
        RoundPanel p = new RoundPanel(new BorderLayout(0, 4), Theme.CARD).accent(color);
        p.setBorder(Theme.pad(14, 22));
        p.add(Theme.label(title, Theme.SMALL, Theme.MUTED), BorderLayout.NORTH);
        p.add(Theme.label(value, Theme.TITLE, Theme.shade(color, 0.1)), BorderLayout.CENTER);
        return p;
    }

    private static JComponent subjectCard(Subject s, MainFrame frame) {
        Color c = Theme.parse(s.getColor(), Theme.INDIGO);
        RoundPanel card = new RoundPanel(new BorderLayout(0, 10), Theme.CARD);
        card.setBorder(Theme.pad(16, 16));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setOpaque(false);
        top.add(new Badge(s.getShortCode(), c, 46), BorderLayout.WEST);
        // text areas re-wrap to the card's real width, so nothing is clipped on smaller windows
        JPanel nameBox = new JPanel(new java.awt.GridBagLayout());   // vertically centres the name next to the badge
        nameBox.setOpaque(false);
        java.awt.GridBagConstraints gc = new java.awt.GridBagConstraints();
        gc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        nameBox.add(UiUtil.textBlock(s.getName(), Theme.HEADING, Theme.TEXT), gc);
        top.add(nameBox, BorderLayout.CENTER);
        card.add(top, BorderLayout.NORTH);

        card.add(UiUtil.textBlock(s.getDescription() == null ? "" : s.getDescription(), Theme.SMALL, Theme.MUTED),
                BorderLayout.CENTER);

        JPanel foot = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        foot.setOpaque(false);
        foot.add(Chip.soft(s.getSubtopicCount() + " topics", c));
        foot.add(Chip.soft(s.getTotalQuestions() + " Qs", Theme.MUTED));
        card.add(foot, BorderLayout.SOUTH);
        card.setPreferredSize(new Dimension(220, 178));

        MouseAdapter m = new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { card.border(c, 2); }
            @Override public void mouseExited(MouseEvent e) {
                // the event may come from a child, so convert its point into the card's coordinates
                java.awt.Point p = javax.swing.SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), card);
                if (!card.contains(p)) card.border(Theme.BORDER, 1);
            }
            @Override public void mouseClicked(MouseEvent e) { frame.showSubject(s); }
        };
        addMouseDeep(card, m);
        setCursorDeep(card, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return card;
    }

    static void setCursorDeep(Component c, Cursor cursor) {
        c.setCursor(cursor);
        if (c instanceof java.awt.Container) {
            for (Component k : ((java.awt.Container) c).getComponents()) setCursorDeep(k, cursor);
        }
    }

    /** Child components swallow mouse events, so register the listener on the whole tree. */
    static void addMouseDeep(Component c, MouseAdapter m) {
        c.addMouseListener(m);
        if (c instanceof java.awt.Container) {
            for (Component k : ((java.awt.Container) c).getComponents()) addMouseDeep(k, m);
        }
    }

    private static JComponent recentRow(QuizResult r, boolean showName, MainFrame frame) {
        RoundPanel row = new RoundPanel(new BorderLayout(14, 0), Theme.CARD).shadow(false);
        row.setBorder(Theme.pad(10, 16));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        left.add(Chip.soft(r.getDifficulty().getLabel(), Theme.color(r.getDifficulty())));
        left.add(Theme.label(r.getTitle(), Theme.BODY_BOLD, Theme.TEXT));
        left.add(Theme.label((showName ? r.getUserName() + " • " : "") + r.getTakenAt().format(QuizResult.DISPLAY_FMT),
                Theme.SMALL, Theme.MUTED));
        row.add(left, BorderLayout.CENTER);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(Theme.label(String.format("%.0f%%", r.getPercentage()), Theme.HEADING,
                r.isPassed() ? Theme.CORRECT : Theme.WRONG));
        PillButton review = new PillButton("Review", PillButton.Style.SECONDARY);
        review.addActionListener(e -> frame.showReview(r, true));
        right.add(review);
        row.add(right, BorderLayout.EAST);
        return row;
    }
}
