package com.quiz.ui;

import com.quiz.exception.DataAccessException;
import com.quiz.model.Question;
import com.quiz.model.QuizResult;
import com.quiz.service.QuizSession;
import com.quiz.ui.components.Chip;
import com.quiz.ui.components.CodeBlock;
import com.quiz.ui.components.OptionCard;
import com.quiz.ui.components.PillButton;
import com.quiz.ui.components.RoundPanel;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * The exam screen. Event-driven: option cards, navigation and palette buttons, the hint button,
 * keyboard shortcuts and a Swing Timer all update the {@link QuizSession}; the screen is then redrawn from it.
 */
public class QuizPanel extends JPanel {

    private final AppContext ctx;
    private final MainFrame frame;
    private final QuizSession session;
    private final Color subjectColor;

    private final JPanel questionHolder = UiUtil.widthTracking(new BorderLayout());
    private final JScrollPane questionScroll;
    private final OptionCard[] optionCards = new OptionCard[Question.OPTION_COUNT];
    private final JButton[] palette;
    private final Chip timerChip = new Chip("00:00", new Color(255, 255, 255, 60), Color.WHITE);
    private final JLabel progressLabel = Theme.label("", Theme.BODY_BOLD, Color.WHITE);
    private final PillButton prevBtn = new PillButton("‹ Previous", PillButton.Style.SECONDARY);
    private final PillButton nextBtn = new PillButton("Next ›", PillButton.Style.PRIMARY);
    private final PillButton clearBtn = new PillButton("Clear", PillButton.Style.SECONDARY);
    private final PillButton markBtn = new PillButton("Mark for review", PillButton.Style.SECONDARY);
    private final PillButton submitBtn = new PillButton("Submit Test", PillButton.Style.WARM);
    private final Timer timer;
    private JLabel markedFlag;
    private boolean submitted;

    public QuizPanel(AppContext ctx, MainFrame frame, QuizSession session) {
        super(new BorderLayout());
        this.ctx = ctx;
        this.frame = frame;
        this.session = session;
        this.subjectColor = Theme.parse(session.getSubject().getColor(), Theme.INDIGO);
        this.palette = new JButton[session.size()];
        setBackground(Theme.BG);

        // ---------- app bar ----------
        double marks = session.getDifficulty().getMarks();
        String sub = session.getDifficulty().getLabel() + " mode  •  "
                + com.quiz.service.AdminService.fmt(marks) + (marks == 1 ? " mark" : " marks") + " per question"
                + (session.getDifficulty().isHintsAllowed() ? "  •  hints available" : "");
        AppBar bar = new AppBar(session.getTitle(), sub, subjectColor, Theme.shade(subjectColor, 0.35));
        timerChip.setFont(Theme.TIMER);
        timerChip.setBorder(Theme.pad(6, 14));
        bar.action(progressLabel).action(timerChip);
        add(bar, BorderLayout.NORTH);

        // ---------- question area ----------
        questionHolder.setBorder(Theme.pad(4, 4));
        questionScroll = UiUtil.scroll(questionHolder);
        // option text is wrapped to a pixel width, so re-wrap it whenever the window is resized
        questionScroll.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int w = optionWrapWidth();
                for (OptionCard c : optionCards) c.setWrapWidth(w);
            }
        });
        RoundPanel qCard = new RoundPanel(new BorderLayout(), Theme.CARD);
        qCard.setBorder(Theme.pad(18, 22));
        qCard.add(questionScroll, BorderLayout.CENTER);
        for (int i = 0; i < Question.OPTION_COUNT; i++) {
            final int idx = i;
            optionCards[i] = new OptionCard(Question.LABELS[i]);
            optionCards[i].setOnClick(() -> choose(idx));
        }

        // ---------- palette ----------
        JPanel pal = new JPanel(new GridLayout(0, 5, 6, 6));
        pal.setOpaque(false);
        for (int i = 0; i < palette.length; i++) {
            final int idx = i;
            JButton b = new JButton(String.valueOf(i + 1));
            b.setFont(Theme.SMALL_BOLD);
            b.setFocusable(false);
            b.setContentAreaFilled(false);   // flat so our status colour is painted by every look-and-feel
            b.setOpaque(true);
            b.setMargin(new Insets(4, 2, 4, 2));
            b.setPreferredSize(new Dimension(42, 34));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.addActionListener(e -> { session.goTo(idx); renderQuestion(); });
            palette[i] = b;
            pal.add(b);
        }
        JPanel palTop = new JPanel(new BorderLayout());
        palTop.setOpaque(false);
        palTop.add(pal, BorderLayout.NORTH);
        RoundPanel palCard = new RoundPanel(new BorderLayout(0, 12), Theme.CARD);
        palCard.setPreferredSize(new Dimension(280, 10));
        palCard.add(Theme.label("Question palette", Theme.HEADING, Theme.TEXT), BorderLayout.NORTH);
        palCard.add(UiUtil.scroll(palTop), BorderLayout.CENTER);
        palCard.add(legend(), BorderLayout.SOUTH);

        JPanel center = new JPanel(new BorderLayout(18, 0));
        center.setOpaque(false);
        center.setBorder(Theme.pad(18, 24));
        center.add(qCard, BorderLayout.CENTER);
        center.add(palCard, BorderLayout.EAST);
        add(center, BorderLayout.CENTER);

        // ---------- navigation ----------
        JPanel nav = new JPanel(new BorderLayout());
        nav.setOpaque(false);
        nav.setBorder(BorderFactory.createEmptyBorder(0, 24, 18, 24));
        JPanel l = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        l.setOpaque(false);
        l.add(prevBtn);
        l.add(nextBtn);
        JPanel m = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        m.setOpaque(false);
        m.add(clearBtn);
        m.add(markBtn);
        JPanel r = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        r.setOpaque(false);
        r.add(submitBtn);
        nav.add(l, BorderLayout.WEST);
        nav.add(m, BorderLayout.CENTER);
        nav.add(r, BorderLayout.EAST);
        add(nav, BorderLayout.SOUTH);

        prevBtn.addActionListener(e -> { session.previous(); renderQuestion(); });
        nextBtn.addActionListener(e -> { session.next(); renderQuestion(); });
        clearBtn.addActionListener(e -> clear());
        markBtn.addActionListener(e -> { session.toggleMarked(); updateState(); });
        submitBtn.addActionListener(e -> submit(false, true));
        installKeyBindings();

        timer = new Timer(1000, e -> tick());
        timer.setInitialDelay(0);
    }

    /** Called once the panel is on screen. */
    public void begin() {
        renderQuestion();
        timer.start();
        // take keyboard focus so the A-D / arrow shortcuts work immediately
        setFocusable(true);
        requestFocusInWindow();
    }

    // ================================================================== rendering

    /** Rebuilds the question card for the current question (navigation). */
    private void renderQuestion() {
        int i = session.getCurrentIndex();
        Question q = session.getCurrentQuestion();

        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        markedFlag = Theme.label("⚑ Marked for review", Theme.SMALL_BOLD, Theme.shade(Theme.AMBER, 0.2));
        JPanel head = UiUtil.row(8, Theme.label("Question " + (i + 1) + " of " + session.size(), Theme.HEADING, Theme.TEXT),
                q.getSubtopicName() == null ? null : Chip.soft(q.getSubtopicName(), subjectColor), markedFlag);
        box.add(UiUtil.capHeight(head));
        box.add(Box.createVerticalStrut(12));

        box.add(UiUtil.capHeight(UiUtil.textBlock(q.getText(), Theme.QUESTION, Theme.TEXT)));
        if (q.hasCode()) {
            box.add(UiUtil.capHeight(new CodeBlock(q.getCode(), q.getCodeLang(), 18, false)));
        } else {
            box.add(Box.createVerticalStrut(12));
        }

        if (session.isHintAvailable(i)) {
            box.add(UiUtil.capHeight(hintSection(q)));
            box.add(Box.createVerticalStrut(12));
        }

        int wrap = optionWrapWidth();
        for (int k = 0; k < Question.OPTION_COUNT; k++) {
            optionCards[k].setOption(q.getOption(k), q.hasCode(), wrap);
            box.add(UiUtil.capHeight(optionCards[k]));
            box.add(Box.createVerticalStrut(10));
        }

        questionHolder.removeAll();
        questionHolder.add(box, BorderLayout.NORTH);
        questionHolder.revalidate();
        questionHolder.repaint();
        UiUtil.relayoutLater(questionHolder);
        SwingUtilities.invokeLater(() -> questionScroll.getVerticalScrollBar().setValue(0));
        updateState();
    }

    /** Width available for option text: the visible question area minus the letter bubble and padding. */
    private int optionWrapWidth() {
        int w = questionScroll.getViewport().getWidth();
        return w <= 0 ? 600 : Math.max(240, w - 120);
    }

    private JComponent hintSection(Question q) {
        int i = session.getCurrentIndex();
        RoundPanel hint = new RoundPanel(new BorderLayout(12, 0), Theme.AMBER_BG).border(Theme.tint(Theme.AMBER, 0.5)).shadow(false).radius(14);
        hint.setBorder(Theme.pad(12, 16));
        if (session.isHintShown(i)) {
            JPanel text = new JPanel(new BorderLayout(0, 4));
            text.setOpaque(false);
            text.add(Theme.label("Hint", Theme.SMALL_BOLD, Theme.shade(Theme.AMBER, 0.35)), BorderLayout.NORTH);
            text.add(UiUtil.textBlock(q.getHint(), Theme.BODY, Theme.TEXT), BorderLayout.CENTER);
            hint.add(text, BorderLayout.CENTER);
        } else {
            hint.add(Theme.label("Stuck? Reveal a starting hint for this question. (Hints are recorded in your result.)",
                    Theme.SMALL, Theme.shade(Theme.AMBER, 0.45)), BorderLayout.CENTER);
            PillButton show = new PillButton("Show hint", PillButton.Style.WARM).color(Theme.AMBER);
            show.addActionListener(e -> revealHint());
            hint.add(show, BorderLayout.EAST);
        }
        return hint;
    }

    /** Light-weight refresh after answering / marking (no rebuild). */
    private void updateState() {
        int i = session.getCurrentIndex();
        int selected = session.getAnswer(i);
        for (int k = 0; k < Question.OPTION_COUNT; k++) optionCards[k].setSelected(k == selected);
        if (markedFlag != null) markedFlag.setVisible(session.isMarked(i));

        for (int k = 0; k < palette.length; k++) {
            JButton b = palette[k];
            Color bg;
            Color fg = Color.WHITE;
            if (session.isMarked(k)) bg = Theme.AMBER;
            else if (session.isAnswered(k)) bg = Theme.TEAL;
            else { bg = Theme.SKIPPED_BG; fg = Theme.TEXT; }
            b.setBackground(bg);
            b.setForeground(fg);
            b.setBorder(k == i ? BorderFactory.createLineBorder(Theme.INDIGO, 3)
                    : BorderFactory.createLineBorder(Theme.shade(bg, 0.08), 1));
        }
        prevBtn.setEnabled(session.hasPrevious());
        nextBtn.setEnabled(session.hasNext());
        clearBtn.setEnabled(selected >= 0);
        markBtn.setText(session.isMarked(i) ? "Unmark" : "Mark for review");
        progressLabel.setText("Answered " + session.answeredCount() + " / " + session.size()
                + (session.hintsUsed() > 0 ? "   •   Hints " + session.hintsUsed() : ""));
    }

    // ================================================================== events

    private void choose(int idx) {
        if (submitted) return;
        session.selectAnswer(idx);
        updateState();
    }

    private void clear() {
        if (submitted || !session.isAnswered(session.getCurrentIndex())) return;
        session.clearAnswer();
        updateState();
    }

    private void revealHint() {
        if (submitted) return;
        session.revealHint();
        renderQuestion();
    }

    private void tick() {
        if (submitted) return;
        if (session.hasTimeLimit()) {
            int rem = session.getRemainingSeconds();
            timerChip.setText("⏱ " + QuizResult.formatDuration(rem));
            timerChip.setFill(rem <= 60 ? Theme.ROSE : rem <= 180 ? Theme.ORANGE : new Color(255, 255, 255, 60));
            if (rem == 0) submit(true, true);
        } else {
            timerChip.setText("⏱ " + QuizResult.formatDuration(session.getElapsedSeconds()));
        }
    }

    /**
     * Ends the attempt.
     * @param auto     true when triggered by the timer (no confirmation asked)
     * @param navigate true to open the result screen afterwards
     */
    public void submit(boolean auto, boolean navigate) {
        if (submitted) return;
        if (!auto && navigate) {
            List<Integer> missing = session.unansweredNumbers();
            String msg;
            if (!missing.isEmpty()) {
                msg = "<html>You have <b>" + missing.size() + " unanswered</b> question(s): " + abbreviate(missing)
                        + "<br>Unanswered questions score 0.<br><br>Submit anyway?</html>";
            } else if (session.markedCount() > 0) {
                msg = "<html>All questions are answered, but " + session.markedCount()
                        + " are still marked for review.<br><br>Submit now?</html>";
            } else {
                msg = "Submit your answers? You cannot change them afterwards.";
            }
            if (!UiUtil.confirm(this, msg, "Submit Test")) return;
            if (submitted) return; // the timer may have auto-submitted while the dialog was open
        }
        submitted = true;
        timer.stop();
        QuizResult result = ctx.quiz().evaluate(session, auto);
        try {
            ctx.quiz().save(result);
        } catch (DataAccessException ex) {
            UiUtil.error(this, "Result not saved", "Your score was calculated but could not be saved:\n" + ex.getMessage());
        }
        if (auto) UiUtil.info(this, "Time is up! Your test was submitted automatically.");
        if (navigate) frame.showResult(result);
    }

    public boolean isSubmitted() {
        return submitted;
    }

    private static String abbreviate(List<Integer> nums) {
        if (nums.size() <= 12) return nums.toString().replaceAll("[\\[\\]]", "");
        return nums.subList(0, 12).toString().replaceAll("[\\[\\]]", "") + " …";
    }

    private JPanel legend() {
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 6));
        p.setOpaque(false);
        p.add(legendItem(Theme.TEAL, "Answered"));
        p.add(legendItem(Theme.AMBER, "Marked for review"));
        p.add(legendItem(Theme.SKIPPED_BG, "Not answered"));
        p.add(Theme.label("<html>Keys: A-D / 1-4 answer, ← → move,<br>M mark, Delete clear"
                + (session.getDifficulty().isHintsAllowed() ? ", H hint" : "") + "</html>", Theme.SMALL, Theme.MUTED));
        return p;
    }

    private static JPanel legendItem(Color c, String text) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        JLabel box = new JLabel();
        box.setOpaque(true);
        box.setBackground(c);
        box.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        box.setPreferredSize(new Dimension(16, 16));
        row.add(box);
        row.add(Theme.label(text, Theme.SMALL, Theme.MUTED));
        return row;
    }

    private void installKeyBindings() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        for (int k = 0; k < Question.OPTION_COUNT; k++) {
            final int idx = k;
            String name = "choose" + k;
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A + k, 0), name);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_1 + k, 0), name);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD1 + k, 0), name);
            getActionMap().put(name, action(() -> choose(idx)));
        }
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "next");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "prev");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0), "mark");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_H, 0), "hint");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "clear");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "clear");
        getActionMap().put("next", action(() -> { if (!submitted && session.hasNext()) { session.next(); renderQuestion(); } }));
        getActionMap().put("prev", action(() -> { if (!submitted && session.hasPrevious()) { session.previous(); renderQuestion(); } }));
        getActionMap().put("mark", action(() -> { if (!submitted) { session.toggleMarked(); updateState(); } }));
        getActionMap().put("hint", action(() -> {
            int i = session.getCurrentIndex();
            if (!submitted && session.isHintAvailable(i) && !session.isHintShown(i)) revealHint();
        }));
        getActionMap().put("clear", action(this::clear));
    }

    private static AbstractAction action(Runnable r) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                r.run();
            }
        };
    }
}
