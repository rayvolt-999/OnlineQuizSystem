package com.quiz.ui;

import com.quiz.model.AnswerRecord;
import com.quiz.model.Question;
import com.quiz.model.QuizResult;
import com.quiz.ui.components.Chip;
import com.quiz.ui.components.CodeBlock;
import com.quiz.ui.components.PillButton;
import com.quiz.ui.components.RoundPanel;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;

/** Every question of an attempt with the student's answer, the correct answer, hint usage and an explanation. */
public class ReviewPanel extends JPanel {

    private static final String[] FILTERS = {"All questions", "Correct", "Wrong", "Unanswered", "Hint used"};

    private final QuizResult result;
    private final JPanel list = UiUtil.widthTracking(null);
    private final JScrollPane scroll;

    public ReviewPanel(MainFrame frame, QuizResult result, boolean fromHistory) {
        super(new BorderLayout());
        this.result = result;
        setBackground(Theme.BG);

        AppBar bar = new AppBar("Answer Review — " + result.getTitle(), result.getUserName() + "  •  "
                + result.getDifficulty().getLabel() + " mode  •  " + result.getTakenAt().format(QuizResult.DISPLAY_FMT)
                + "  •  " + String.format("%.0f%%", result.getPercentage()));
        JComboBox<String> filter = new JComboBox<>(FILTERS);
        filter.setPrototypeDisplayValue("All questions    ");
        PillButton back = new PillButton(fromHistory ? "‹ Back" : "‹ Back to score", PillButton.Style.GHOST);
        bar.action(Theme.label("Show", Theme.SMALL_BOLD, Color.WHITE)).action(filter).action(back);
        add(bar, BorderLayout.NORTH);

        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBorder(Theme.pad(18, 26));
        scroll = UiUtil.scroll(list);
        add(scroll, BorderLayout.CENTER);

        filter.addActionListener(e -> render(filter.getSelectedIndex()));
        back.addActionListener(e -> {
            if (fromHistory) frame.showHistory(); else frame.backToResult(result);
        });
        render(0);
    }

    private void render(int filterIndex) {
        list.removeAll();
        int n = 0;
        int shown = 0;
        for (AnswerRecord a : result.getAnswers()) {
            n++;
            AnswerRecord.Status s = a.getStatus();
            if (filterIndex == 1 && s != AnswerRecord.Status.CORRECT) continue;
            if (filterIndex == 2 && s != AnswerRecord.Status.WRONG) continue;
            if (filterIndex == 3 && s != AnswerRecord.Status.UNANSWERED) continue;
            if (filterIndex == 4 && !a.isHintUsed()) continue;
            list.add(UiUtil.capHeight(card(n, a)));
            list.add(Box.createVerticalStrut(14));
            shown++;
        }
        if (shown == 0) list.add(UiUtil.capHeight(Theme.label("No questions in this category.", Theme.BODY, Theme.MUTED)));
        list.revalidate();
        list.repaint();
        UiUtil.relayoutLater(list);
        SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0));
    }

    private JComponent card(int number, AnswerRecord a) {
        Question q = a.getQuestion();
        Color edge;
        String tag;
        switch (a.getStatus()) {
            case CORRECT: edge = Theme.TEAL; tag = "✔ Correct"; break;
            case WRONG: edge = Theme.ROSE; tag = "✘ Wrong"; break;
            default: edge = Theme.SKIPPED; tag = "— Not answered"; break;
        }
        RoundPanel card = new RoundPanel(new BorderLayout(), Theme.CARD).accent(edge);
        card.setBorder(Theme.pad(16, 24));
        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        head.setOpaque(false);
        head.add(Theme.label("Q" + number, Theme.HEADING, Theme.TEXT));
        head.add(new Chip(tag, edge, Color.WHITE));
        if (q.getSubtopicName() != null) head.add(Chip.soft(q.getSubtopicName(), Theme.INDIGO));
        if (a.isHintUsed()) head.add(Chip.soft("Hint used", Theme.AMBER));
        box.add(UiUtil.capHeight(head));
        box.add(Box.createVerticalStrut(10));
        box.add(UiUtil.capHeight(UiUtil.textBlock(q.getText(), Theme.BODY_BOLD, Theme.TEXT)));
        if (q.hasCode()) box.add(UiUtil.capHeight(new CodeBlock(q.getCode(), q.getCodeLang(), 30)));
        else box.add(Box.createVerticalStrut(8));

        for (int i = 0; i < Question.OPTION_COUNT; i++) {
            boolean isCorrect = i == q.getCorrectIndex();
            boolean isChosen = i == a.getSelectedIndex();
            String suffix = isCorrect && isChosen ? "your answer ✔" : isCorrect ? "correct answer" : isChosen ? "your answer" : null;
            Color bg = isCorrect ? Theme.CORRECT_BG : isChosen ? Theme.WRONG_BG : Theme.CARD;
            Color fg = isCorrect ? Theme.CORRECT : isChosen ? Theme.WRONG : Theme.TEXT;
            RoundPanel row = new RoundPanel(new BorderLayout(10, 0), bg).border(isCorrect || isChosen ? null : Theme.BORDER).shadow(false).radius(12);
            row.setBorder(Theme.pad(7, 12));
            JLabel text = Theme.label("<html>" + Question.label(i) + ".&nbsp; " + UiUtil.escape(q.getOption(i)) + "</html>",
                    q.hasCode() ? Theme.CODE : Theme.BODY, fg);
            row.add(text, BorderLayout.CENTER);
            if (suffix != null) row.add(Theme.label(suffix, Theme.SMALL_BOLD, fg), BorderLayout.EAST);
            box.add(UiUtil.capHeight(row));
            box.add(Box.createVerticalStrut(5));
        }

        if (q.getExplanation() != null || q.getHint() != null) {
            box.add(Box.createVerticalStrut(6));
            RoundPanel exp = new RoundPanel(new BorderLayout(0, 4), Theme.INFO_BG).border(null).shadow(false).radius(12);
            exp.setBorder(Theme.pad(10, 14));
            JPanel inner = new JPanel();
            inner.setOpaque(false);
            inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
            if (q.getExplanation() != null) {
                inner.add(UiUtil.capHeight(Theme.label("Explanation", Theme.SMALL_BOLD, Theme.shade(Theme.SKY, 0.4))));
                inner.add(UiUtil.capHeight(UiUtil.textBlock(q.getExplanation(), Theme.BODY, Theme.TEXT)));
            }
            if (q.getHint() != null && q.getDifficulty().isHintsAllowed()) {
                if (q.getExplanation() != null) inner.add(Box.createVerticalStrut(6));
                inner.add(UiUtil.capHeight(Theme.label("Hint", Theme.SMALL_BOLD, Theme.shade(Theme.AMBER, 0.35))));
                inner.add(UiUtil.capHeight(UiUtil.textBlock(q.getHint(), Theme.SMALL, Theme.MUTED)));
            }
            exp.add(inner);
            box.add(UiUtil.capHeight(exp));
        }
        card.add(box);
        return card;
    }
}
