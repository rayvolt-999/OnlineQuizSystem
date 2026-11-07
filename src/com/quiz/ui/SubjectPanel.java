package com.quiz.ui;

import com.quiz.model.Difficulty;
import com.quiz.model.Subject;
import com.quiz.model.Subtopic;
import com.quiz.service.QuizService;
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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Test set-up: pick a subtopic (or all) and a mode (Easy / Medium / Hard), then start. */
public class SubjectPanel extends JPanel {

    private final Subject subject;
    private final Color color;
    private final List<Subtopic> subtopics = new ArrayList<>();
    private final List<RoundPanel> topicRows = new ArrayList<>();
    private final Map<Difficulty, RoundPanel> modeCards = new EnumMap<>(Difficulty.class);
    private final Map<Difficulty, JLabel> availLabels = new EnumMap<>(Difficulty.class);
    private final JLabel summary = Theme.label(" ", Theme.BODY, Theme.TEXT);
    private final PillButton start = new PillButton("Start Test  ▶", PillButton.Style.WARM);
    private static final Map<Difficulty, String[]> POINTS = new EnumMap<>(Difficulty.class);
    static {
        POINTS.put(Difficulty.EASY, new String[]{"Definitions and terminology", "Basic facts, commands and syntax", "About 30 seconds per question"});
        POINTS.put(Difficulty.MEDIUM, new String[]{"Predict the output of code", "Concepts applied to examples", "A starting hint on every question"});
        POINTS.put(Difficulty.HARD, new String[]{"Edge cases and tricky code", "Multi-step reasoning and numericals", "A starting hint on every question"});
    }

    private Subtopic selectedTopic;          // null = all subtopics
    private Difficulty selectedMode = Difficulty.EASY;

    public SubjectPanel(AppContext ctx, MainFrame frame, Subject subject) {
        super(new BorderLayout());
        this.subject = subject;
        this.color = Theme.parse(subject.getColor(), Theme.INDIGO);
        setBackground(Theme.BG);
        UiUtil.run(this, () -> subtopics.addAll(ctx.quiz().getSubtopics(subject)));

        AppBar bar = new AppBar(subject.getName(), subject.getDescription(), color, Theme.shade(color, 0.35));
        PillButton back = new PillButton("‹ All subjects", PillButton.Style.GHOST);
        back.addActionListener(e -> frame.showDashboard());
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        left.add(new Badge(subject.getShortCode(), Theme.tint(color, 0.15), 48));
        bar.left(left).action(back);
        add(bar, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(20, 0));
        body.setOpaque(false);
        body.setBorder(Theme.pad(22, 26));
        body.add(topicColumn(), BorderLayout.WEST);
        body.add(modeColumn(), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        start.addActionListener(e -> frame.startQuiz(subject, selectedTopic, selectedMode));
        UiUtil.relayoutLater(this);
        selectTopic(-1);
        selectMode(Difficulty.EASY);
    }

    // ------------------------------------------------------------ subtopics

    private JComponent topicColumn() {
        RoundPanel card = new RoundPanel(new BorderLayout(0, 12), Theme.CARD);
        card.setPreferredSize(new Dimension(330, 10));
        card.add(stepTitle("1", "Choose a subtopic"), BorderLayout.NORTH);
        JPanel list = UiUtil.widthTracking(null);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.add(UiUtil.capHeight(topicRow(-1, "All subtopics (mixed)", subject.getQuestionCount(Difficulty.EASY),
                subject.getQuestionCount(Difficulty.MEDIUM), subject.getQuestionCount(Difficulty.HARD))));
        for (int i = 0; i < subtopics.size(); i++) {
            Subtopic t = subtopics.get(i);
            list.add(Box.createVerticalStrut(8));
            list.add(UiUtil.capHeight(topicRow(i, t.getName(), t.getQuestionCount(Difficulty.EASY),
                    t.getQuestionCount(Difficulty.MEDIUM), t.getQuestionCount(Difficulty.HARD))));
        }
        card.add(UiUtil.scroll(list), BorderLayout.CENTER);
        return card;
    }

    private RoundPanel topicRow(int index, String name, int e, int m, int h) {
        RoundPanel row = new RoundPanel(new BorderLayout(8, 4), Theme.CARD).shadow(false).radius(14);
        row.setBorder(Theme.pad(10, 14));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.add(Theme.label(name, Theme.BODY_BOLD, Theme.TEXT), BorderLayout.NORTH);
        JLabel counts = Theme.label("Easy " + e + "  •  Medium " + m + "  •  Hard " + h, Theme.SMALL, Theme.MUTED);
        row.add(counts, BorderLayout.CENTER);
        topicRows.add(row);
        DashboardPanel.addMouseDeep(row, new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent ev) { selectTopic(index); }
        });
        return row;
    }

    private void selectTopic(int index) {
        selectedTopic = index < 0 ? null : subtopics.get(index);
        for (int i = 0; i < topicRows.size(); i++) {
            boolean sel = i == index + 1;
            topicRows.get(i).fill(sel ? Theme.tint(color, 0.88) : Theme.CARD).border(sel ? color : Theme.BORDER, sel ? 2 : 1);
        }
        refresh();
    }

    // ------------------------------------------------------------ modes

    private JComponent modeColumn() {
        JPanel col = new JPanel(new BorderLayout(0, 16));
        col.setOpaque(false);
        JPanel title = new JPanel(new BorderLayout());
        title.setOpaque(false);
        title.add(stepTitle("2", "Choose a mode"), BorderLayout.WEST);
        col.add(title, BorderLayout.NORTH);

        JPanel modes = new JPanel(new GridLayout(1, 3, 16, 0));
        modes.setOpaque(false);
        for (Difficulty d : Difficulty.values()) modes.add(modeCard(d));
        col.add(modes, BorderLayout.CENTER);

        RoundPanel footer = new RoundPanel(new BorderLayout(16, 0), Theme.CARD);
        footer.setBorder(Theme.pad(14, 20));
        footer.add(summary, BorderLayout.CENTER);
        footer.add(start, BorderLayout.EAST);
        col.add(footer, BorderLayout.SOUTH);
        return col;
    }

    private JComponent modeCard(Difficulty d) {
        Color c = Theme.color(d);
        RoundPanel card = new RoundPanel(new BorderLayout(0, 10), Theme.CARD);
        card.setBorder(Theme.pad(18, 18));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(Theme.label(d.getLabel(), Theme.TITLE, Theme.shade(c, 0.15)), BorderLayout.WEST);
        head.add(new Chip(d.isHintsAllowed() ? "Hints" : "No hints", d.isHintsAllowed() ? Theme.AMBER_BG : Theme.SKIPPED_BG,
                d.isHintsAllowed() ? Theme.shade(Theme.AMBER, 0.3) : Theme.MUTED), BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);

        JPanel facts = new JPanel();
        facts.setOpaque(false);
        facts.setLayout(new BoxLayout(facts, BoxLayout.Y_AXIS));
        facts.add(bigFact(String.valueOf(d.getQuestionCount()), "questions", c));
        facts.add(Box.createVerticalStrut(6));
        facts.add(bigFact(String.valueOf(d.getMinutes()), "minutes", c));
        facts.add(Box.createVerticalStrut(6));
        facts.add(bigFact(com.quiz.service.AdminService.fmt(d.getMarks()), d.getMarks() == 1 ? "mark each" : "marks each", c));
        facts.add(Box.createVerticalStrut(14));
        // text areas re-wrap to the card's real width, so nothing is cut off on a small window
        facts.add(UiUtil.capHeight(UiUtil.textBlock(d.getFocus(), Theme.BODY_BOLD, Theme.TEXT)));
        facts.add(Box.createVerticalStrut(4));
        facts.add(UiUtil.capHeight(UiUtil.textBlock(d.getPurpose(), Theme.SMALL, Theme.MUTED)));
        facts.add(Box.createVerticalStrut(14));
        for (String point : POINTS.get(d)) {
            JPanel row = new JPanel(new BorderLayout(6, 0));
            row.setOpaque(false);
            JLabel dot = Theme.label("\u25CF", Theme.SMALL, c);
            dot.setVerticalAlignment(JLabel.TOP);
            row.add(dot, BorderLayout.WEST);
            row.add(UiUtil.textBlock(point, Theme.SMALL, Theme.TEXT), BorderLayout.CENTER);
            facts.add(UiUtil.capHeight(row));
            facts.add(Box.createVerticalStrut(6));
        }
        JPanel factsTop = new JPanel(new BorderLayout());   // keeps the facts at their natural height (no stretched gaps)
        factsTop.setOpaque(false);
        factsTop.add(facts, BorderLayout.NORTH);
        card.add(factsTop, BorderLayout.CENTER);

        JLabel avail = Theme.label(" ", Theme.SMALL_BOLD, Theme.MUTED);
        availLabels.put(d, avail);
        card.add(avail, BorderLayout.SOUTH);

        modeCards.put(d, card);
        DashboardPanel.addMouseDeep(card, new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { selectMode(d); }
        });
        DashboardPanel.setCursorDeep(card, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return card;
    }

    private static JComponent bigFact(String value, String unit, Color c) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        p.setOpaque(false);
        p.add(Theme.label(value, Theme.TITLE, Theme.TEXT));
        p.add(Theme.label(unit, Theme.BODY, Theme.MUTED));
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height));
        return p;
    }

    private void selectMode(Difficulty d) {
        selectedMode = d;
        for (Map.Entry<Difficulty, RoundPanel> e : modeCards.entrySet()) {
            boolean sel = e.getKey() == d;
            Color c = Theme.color(e.getKey());
            e.getValue().fill(sel ? Theme.tint(c, 0.88) : Theme.CARD).border(sel ? c : Theme.BORDER, sel ? 3 : 1).accent(sel ? c : null);
        }
        refresh();
    }

    // ------------------------------------------------------------ summary

    private int available(Difficulty d) {
        return selectedTopic == null ? subject.getQuestionCount(d) : selectedTopic.getQuestionCount(d);
    }

    private void refresh() {
        for (Difficulty d : Difficulty.values()) {
            JLabel l = availLabels.get(d);
            if (l == null) continue;
            int a = available(d);
            l.setText(a == 0 ? "No questions available" : a + " available in the bank");
            l.setForeground(a == 0 ? Theme.WRONG : Theme.MUTED);
        }
        if (selectedMode == null) return;
        int a = available(selectedMode);
        int n = QuizService.plannedQuestionCount(a, selectedMode);
        String where = selectedTopic == null ? "all subtopics" : selectedTopic.getName();
        if (n == 0) {
            summary.setText("<html><b>No " + selectedMode.getLabel() + " questions</b> for " + UiUtil.escape(where)
                    + " yet. Pick another subtopic or mode.</html>");
        } else {
            String note = n < selectedMode.getQuestionCount()
                    ? " <span style='color:#B45309'>(only " + n + " of " + selectedMode.getQuestionCount() + " available here; choose 'All subtopics' for the full test)</span>"
                    : "";
            summary.setText("<html><b>" + selectedMode.getLabel() + "</b> test on <b>" + UiUtil.escape(where) + "</b>: "
                    + n + " questions, " + selectedMode.getMinutes() + " minutes, max "
                    + com.quiz.service.AdminService.fmt(n * selectedMode.getMarks()) + " marks." + note + "</html>");
        }
        start.setEnabled(n > 0);
    }

    private JComponent stepTitle(String n, String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        p.setOpaque(false);
        p.add(new Chip(n, color, Color.WHITE));
        p.add(Theme.label(text, Theme.HEADING, Theme.TEXT));
        return p;
    }
}
