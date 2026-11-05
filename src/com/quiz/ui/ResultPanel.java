package com.quiz.ui;

import com.quiz.model.QuizResult;
import com.quiz.model.Subject;
import com.quiz.model.Subtopic;
import com.quiz.service.AdminService;
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
import java.awt.GridBagLayout;
import java.awt.GridLayout;

/** Score summary shown right after submitting a test. */
public class ResultPanel extends JPanel {

    public ResultPanel(AppContext ctx, MainFrame frame, QuizResult r) {
        super(new BorderLayout());
        setBackground(Theme.BG);
        Color mode = Theme.color(r.getDifficulty());
        add(new AppBar("Result — " + r.getTitle(), r.getDifficulty().getLabel() + " mode  •  "
                + r.getTakenAt().format(QuizResult.DISPLAY_FMT)), BorderLayout.NORTH);

        RoundPanel card = new RoundPanel(new BorderLayout(34, 0), Theme.CARD);
        card.setBorder(Theme.pad(28, 34));
        card.add(new ScoreChart(r.getCorrect(), r.getWrong(), r.getUnanswered(),
                String.format("%.0f%%", r.getPercentage()), "score"), BorderLayout.WEST);

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        JPanel verdict = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        verdict.setOpaque(false);
        verdict.add(Theme.label(r.isPassed() ? "Well done!" : "Keep practising!", Theme.TITLE, r.isPassed() ? Theme.CORRECT : Theme.ORANGE));
        verdict.add(new Chip("Grade " + r.getGrade(), r.isPassed() ? Theme.CORRECT : Theme.ROSE, Color.WHITE));
        verdict.add(Chip.soft(r.getDifficulty().getLabel(), mode));
        if (r.isAutoSubmitted()) verdict.add(Chip.soft("Auto-submitted (time up)", Theme.ROSE));
        info.add(UiUtil.capHeight(verdict));
        info.add(Box.createVerticalStrut(18));

        JPanel tiles = new JPanel(new GridLayout(2, 4, 12, 12));
        tiles.setOpaque(false);
        tiles.add(tile("Score", AdminService.fmt(r.getScore()) + " / " + AdminService.fmt(r.getMaxScore()), Theme.INDIGO));
        tiles.add(tile("Correct", String.valueOf(r.getCorrect()), Theme.TEAL));
        tiles.add(tile("Wrong", String.valueOf(r.getWrong()), Theme.ROSE));
        tiles.add(tile("Unanswered", String.valueOf(r.getUnanswered()), Theme.SKIPPED));
        tiles.add(tile("Accuracy", String.format("%.0f%%", r.getAccuracy()), Theme.SKY));
        tiles.add(tile("Hints used", r.getHintsUsed() + (r.getDifficulty().isHintsAllowed() ? "" : " (n/a)"), Theme.AMBER));
        tiles.add(tile("Time taken", QuizResult.formatDuration(r.getTimeTakenSeconds()), Theme.ORANGE));
        tiles.add(tile("Questions", String.valueOf(r.getTotalQuestions()), Theme.MUTED));
        info.add(UiUtil.capHeight(tiles));
        info.add(Box.createVerticalStrut(14));
        info.add(UiUtil.capHeight(Theme.label("Accuracy = correct / attempted. Unanswered questions score 0; hints never cost marks.",
                Theme.SMALL, Theme.MUTED)));
        card.add(info, BorderLayout.CENTER);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.setBorder(Theme.pad(20, 26));
        center.add(card);
        add(center, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        actions.setOpaque(false);
        actions.setBorder(Theme.pad(0, 0));
        actions.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 24, 0));
        PillButton home = new PillButton("Dashboard", PillButton.Style.SECONDARY);
        PillButton other = new PillButton("Change mode / topic", PillButton.Style.SECONDARY);
        PillButton retake = new PillButton("Retake", PillButton.Style.PRIMARY);
        PillButton review = new PillButton("Review Answers", PillButton.Style.WARM);
        actions.add(home);
        actions.add(other);
        actions.add(retake);
        actions.add(review);
        add(actions, BorderLayout.SOUTH);

        review.addActionListener(e -> frame.showReview(r, false));
        home.addActionListener(e -> frame.showDashboard());
        other.addActionListener(e -> UiUtil.run(this, () -> {
            Subject s = ctx.quiz().getSubject(r.getSubjectId());
            if (s == null) UiUtil.warn(this, "This subject no longer exists."); else frame.showSubject(s);
        }));
        retake.addActionListener(e -> UiUtil.run(this, () -> {
            Subject s = ctx.quiz().getSubject(r.getSubjectId());
            if (s == null) {
                UiUtil.warn(this, "This subject no longer exists.");
                return;
            }
            Subtopic topic = null;
            if (r.getSubtopicId() > 0) {
                for (Subtopic t : ctx.quiz().getSubtopics(s)) if (t.getId() == r.getSubtopicId()) topic = t;
            }
            frame.startQuiz(s, topic, r.getDifficulty());
        }));
    }

    private static JComponent tile(String title, String value, Color c) {
        RoundPanel p = new RoundPanel(new BorderLayout(0, 2), Theme.tint(c, 0.9)).border(null).shadow(false).radius(14);
        p.setBorder(Theme.pad(10, 14));
        p.add(Theme.label(title, Theme.SMALL, Theme.shade(c, 0.3)), BorderLayout.NORTH);
        p.add(Theme.label(value, Theme.HEADING, Theme.shade(c, 0.35)), BorderLayout.CENTER);
        return p;
    }
}
