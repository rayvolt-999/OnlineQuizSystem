package com.quiz.ui;

import com.quiz.model.Difficulty;
import com.quiz.model.QuizResult;
import com.quiz.model.Subject;
import com.quiz.model.TopicStat;
import com.quiz.model.User;
import com.quiz.ui.components.BarChart;
import com.quiz.ui.components.Chip;
import com.quiz.ui.components.PillButton;
import com.quiz.ui.components.RoundPanel;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Performance analytics: average score per subject and per mode, plus weak and strong subtopics
 * computed from every individual answer. Students see their own data; admins see the whole class.
 */
public class AnalyticsPanel extends JPanel {

    /** Subtopics need at least this many answered questions before they are judged. */
    static final int MIN_ATTEMPTS = 3;

    public AnalyticsPanel(AppContext ctx, MainFrame frame) {
        super(new BorderLayout());
        setBackground(Theme.BG);
        User user = ctx.getCurrentUser();

        AppBar bar = new AppBar("Performance Analytics", user.isAdmin()
                ? "Class-wide view across all students" : "Where you are strong, and what to practise next");
        PillButton back = new PillButton("‹ Dashboard", PillButton.Style.GHOST);
        PillButton results = new PillButton(user.isAdmin() ? "All Results" : "My Results", PillButton.Style.GHOST);
        bar.action(results).action(back);
        add(bar, BorderLayout.NORTH);
        back.addActionListener(e -> frame.showDashboard());
        results.addActionListener(e -> frame.showHistory());

        List<QuizResult> history = new ArrayList<>();
        List<TopicStat> stats = new ArrayList<>();
        Map<String, Color> subjectColors = new HashMap<>();
        UiUtil.run(this, () -> {
            history.addAll(ctx.quiz().historyFor(user));
            stats.addAll(ctx.quiz().subtopicStats(user));
            for (Subject s : ctx.quiz().getSubjects()) subjectColors.put(s.getName(), Theme.parse(s.getColor(), Theme.INDIGO));
        });

        JPanel body = UiUtil.widthTracking(null);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(Theme.pad(20, 26));

        if (history.isEmpty()) {
            RoundPanel empty = new RoundPanel(new BorderLayout(), Theme.CARD);
            empty.setBorder(Theme.pad(30, 30));
            empty.add(Theme.label("No tests taken yet. Take a few tests and your analytics will appear here.",
                    Theme.HEADING, Theme.MUTED));
            body.add(UiUtil.capHeight(empty));
            add(UiUtil.scroll(body), BorderLayout.CENTER);
            return;
        }

        // ---- headline numbers ----
        int answered = stats.stream().mapToInt(TopicStat::getAttempted).sum();
        int correct = stats.stream().mapToInt(TopicStat::getCorrect).sum();
        int hints = history.stream().mapToInt(QuizResult::getHintsUsed).sum();
        double avg = history.stream().mapToDouble(QuizResult::getPercentage).average().orElse(0);
        JPanel tiles = new JPanel(new GridLayout(1, 4, 16, 0));
        tiles.setOpaque(false);
        tiles.add(tile("Tests taken", String.valueOf(history.size()), Theme.INDIGO));
        tiles.add(tile("Average score", String.format("%.0f%%", avg), Theme.TEAL));
        tiles.add(tile("Questions answered", answered + "  (" + (answered == 0 ? 0 : correct * 100 / answered) + "% right)", Theme.ORANGE));
        tiles.add(tile("Hints used", String.valueOf(hints), Theme.AMBER));
        body.add(UiUtil.capHeight(tiles));
        body.add(Box.createVerticalStrut(18));

        // ---- charts ----
        Map<String, List<QuizResult>> bySubject = history.stream()
                .collect(Collectors.groupingBy(QuizResult::getSubjectName, LinkedHashMap::new, Collectors.toList()));
        BarChart subjChart = new BarChart(190);
        bySubject.forEach((name, list) -> subjChart.add(name,
                list.stream().mapToDouble(QuizResult::getPercentage).average().orElse(0),
                subjectColors.getOrDefault(name, Theme.INDIGO), list.size() + " test(s)"));

        BarChart modeChart = new BarChart(90);
        for (Difficulty d : Difficulty.values()) {
            List<QuizResult> l = history.stream().filter(r -> r.getDifficulty() == d).collect(Collectors.toList());
            modeChart.add(d.getLabel(), l.stream().mapToDouble(QuizResult::getPercentage).average().orElse(0),
                    Theme.color(d), l.isEmpty() ? "not attempted" : l.size() + " test(s)");
        }

        JPanel charts = new JPanel(new GridLayout(1, 2, 16, 0));
        charts.setOpaque(false);
        charts.add(section("Average score by subject", subjChart));
        charts.add(section("Average score by mode", modeChart));
        body.add(UiUtil.capHeight(charts));
        body.add(Box.createVerticalStrut(18));

        // ---- focus areas ----
        List<TopicStat> judged = stats.stream().filter(s -> s.getAttempted() >= MIN_ATTEMPTS).collect(Collectors.toList());
        List<TopicStat> weak = judged.stream().filter(s -> s.getAccuracy() < 60).limit(6).collect(Collectors.toList());
        List<TopicStat> strong = judged.stream().filter(s -> s.getAccuracy() >= 80)
                .sorted((a, b) -> Double.compare(b.getAccuracy(), a.getAccuracy())).limit(6).collect(Collectors.toList());

        JPanel areas = new JPanel(new GridLayout(1, 2, 16, 0));
        areas.setOpaque(false);
        areas.add(section("Focus areas (below 60%)", topicList(weak, Theme.ROSE,
                "No weak subtopics yet" + (judged.isEmpty() ? " (answer at least " + MIN_ATTEMPTS + " questions per subtopic)." : ". Nice!"))));
        areas.add(section("Strong areas (80% and above)", topicList(strong, Theme.TEAL,
                "Keep practising: subtopics where you score 80%+ will appear here.")));
        body.add(UiUtil.capHeight(areas));

        add(UiUtil.scroll(body), BorderLayout.CENTER);
    }

    private static JComponent topicList(List<TopicStat> list, Color c, String emptyText) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        if (list.isEmpty()) {
            p.add(UiUtil.capHeight(Theme.label(UiUtil.wrap(emptyText, 380), Theme.BODY, Theme.MUTED)));
            return p;
        }
        for (TopicStat s : list) {
            RoundPanel row = new RoundPanel(new BorderLayout(10, 0), Theme.tint(c, 0.92)).border(null).shadow(false).radius(12);
            row.setBorder(Theme.pad(8, 12));
            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            left.setOpaque(false);
            left.add(Theme.label(s.getSubtopicName(), Theme.BODY_BOLD, Theme.TEXT));
            left.add(Theme.label(s.getSubjectName(), Theme.SMALL, Theme.MUTED));
            row.add(left, BorderLayout.CENTER);
            row.add(new Chip(String.format("%.0f%%  (%d/%d)", s.getAccuracy(), s.getCorrect(), s.getAttempted()), c, Color.WHITE),
                    BorderLayout.EAST);
            p.add(UiUtil.capHeight(row));
            p.add(Box.createVerticalStrut(6));
        }
        return p;
    }

    private static JComponent section(String title, JComponent content) {
        RoundPanel card = new RoundPanel(new BorderLayout(0, 14), Theme.CARD);
        card.setBorder(Theme.pad(16, 20));
        card.add(Theme.label(title, Theme.HEADING, Theme.TEXT), BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private static JComponent tile(String title, String value, Color c) {
        RoundPanel p = new RoundPanel(new BorderLayout(0, 4), Theme.CARD).accent(c);
        p.setBorder(Theme.pad(14, 22));
        p.add(Theme.label(title, Theme.SMALL, Theme.MUTED), BorderLayout.NORTH);
        p.add(Theme.label(value, Theme.TITLE, Theme.shade(c, 0.1)), BorderLayout.CENTER);
        return p;
    }
}
