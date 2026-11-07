package com.quiz.ui;

import com.quiz.model.Difficulty;
import com.quiz.model.QuizResult;
import com.quiz.model.Subject;
import com.quiz.model.Subtopic;
import com.quiz.service.QuizSession;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * The single application window. Screens are swapped with a CardLayout;
 * every navigation method builds a fresh screen so no stale state leaks between visits.
 */
public class MainFrame extends JFrame {

    private final AppContext ctx;
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private QuizPanel activeQuiz;

    public MainFrame(AppContext ctx) {
        super("Online Examination & Quiz System");
        this.ctx = ctx;
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApplication();
            }
        });
        root.setBackground(Theme.BG);
        setContentPane(root);
        setMinimumSize(new Dimension(1040, 680));
        setSize(1200, 780);
        setLocationRelativeTo(null);
        showLogin();
    }

    private void show(String name, Component screen) {
        // Keep only the visible screen: old screens are dropped so they cannot hold keyboard focus,
        // keep stale data, or pile up in memory.
        root.removeAll();
        screen.setName(name);
        root.add(screen, name);
        cards.show(root, name);
        root.revalidate();
        root.repaint();
    }

    public AppContext context() { return ctx; }

    public void showLogin() {
        ctx.setCurrentUser(null);
        show("login", new LoginPanel(ctx, this));
    }

    public void showDashboard() {
        activeQuiz = null;
        show("dashboard", new DashboardPanel(ctx, this));
    }

    public void showSubject(Subject subject) {
        show("subject", new SubjectPanel(ctx, this, subject));
    }

    /** @param subtopic null = all subtopics mixed */
    public void startQuiz(Subject subject, Subtopic subtopic, Difficulty difficulty) {
        UiUtil.run(this, () -> {
            QuizSession session = ctx.quiz().startQuiz(ctx.getCurrentUser(), subject, subtopic, difficulty);
            activeQuiz = new QuizPanel(ctx, this, session);
            show("quiz", activeQuiz);
            activeQuiz.begin();
        });
    }

    public void showResult(QuizResult result) {
        activeQuiz = null;
        show("result", new ResultPanel(ctx, this, result));
    }

    /** @param fromHistory true if "Back" should return to the history screen instead of the result screen */
    public void showReview(QuizResult result, boolean fromHistory) {
        UiUtil.run(this, () -> {
            ctx.quiz().loadAnswers(result);
            show("review", new ReviewPanel(this, result, fromHistory));
        });
    }

    public void backToResult(QuizResult result) {
        show("result", new ResultPanel(ctx, this, result));
    }

    public void showHistory() {
        show("history", new HistoryPanel(ctx, this));
    }

    public void showAnalytics() {
        show("analytics", new AnalyticsPanel(ctx, this));
    }

    public void showAdmin() {
        show("admin", new AdminPanel(ctx, this));
    }

    private void exitApplication() {
        if (activeQuiz != null && !activeQuiz.isSubmitted()) {
            if (!UiUtil.confirm(this, "A test is in progress. Submit it and exit?", "Exit")) return;
            activeQuiz.submit(false, false);
        }
        dispose();
        System.exit(0);
    }
}
