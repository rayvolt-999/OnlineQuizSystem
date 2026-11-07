package com.quiz.ui;

import com.quiz.db.DataSeeder;
import com.quiz.model.User;
import com.quiz.ui.components.GradientPanel;
import com.quiz.ui.components.PasswordInput;
import com.quiz.ui.components.PillButton;
import com.quiz.ui.components.RoundPanel;
import com.quiz.ui.components.TextInput;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;

/** Split-screen login: colourful welcome panel on the left, sign-in card on the right. */
public class LoginPanel extends JPanel {

    private final AppContext ctx;
    private final MainFrame frame;
    private final TextInput userField = new TextInput(18);
    private final PasswordInput passField = new PasswordInput();

    public LoginPanel(AppContext ctx, MainFrame frame) {
        super(new GridLayout(1, 2));
        this.ctx = ctx;
        this.frame = frame;
        setBackground(Theme.BG);
        add(welcomeSide());
        add(formSide());
        SwingUtilities.invokeLater(() -> userField.getField().requestFocusInWindow());
    }

    private JComponent welcomeSide() {
        GradientPanel p = new GradientPanel(new GridBagLayout(), Theme.INDIGO, Theme.SKY, true);
        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        JLabel tag = Theme.label("B.TECH CSE  •  EXAM PREP", Theme.SMALL_BOLD, new Color(255, 255, 255, 200));
        JLabel title = Theme.label("<html>Online Examination<br>& Quiz System</html>", Theme.DISPLAY, Color.WHITE);
        JLabel tagline = Theme.label("Practise. Analyse. Improve.", Theme.HEADING, new Color(0xFDE68A));
        for (JComponent c : new JComponent[]{tag, title, tagline}) c.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.add(tag);
        box.add(Box.createVerticalStrut(10));
        box.add(title);
        box.add(Box.createVerticalStrut(8));
        box.add(tagline);
        box.add(Box.createVerticalStrut(34));

        String[][] features = {
            {"8", "subjects with focused subtopics"},
            {"3", "modes: Easy, Medium and Hard"},
            {"</>", "code-output questions with hints"},
            {"%", "analytics that find your weak topics"},
        };
        Color[] dots = {Theme.ORANGE, Theme.AMBER, Theme.TEAL, Theme.ROSE};
        for (int i = 0; i < features.length; i++) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
            row.setOpaque(false);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.add(new Dot(features[i][0], dots[i]));
            row.add(Theme.label(features[i][1], Theme.BODY, Color.WHITE));
            box.add(row);
        }
        p.add(box);
        return p;
    }

    private JComponent formSide() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(Theme.BG);

        RoundPanel card = new RoundPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(Theme.pad(30, 34));
        card.setPreferredSize(new Dimension(400, 470));

        JLabel hi = Theme.label("Welcome back", Theme.TITLE, Theme.TEXT);
        JLabel sub = Theme.label("Sign in to continue your preparation", Theme.SMALL, Theme.MUTED);
        JLabel u = Theme.label("Username", Theme.SMALL_BOLD, Theme.TEXT);
        JLabel pw = Theme.label("Password", Theme.SMALL_BOLD, Theme.TEXT);
        PillButton login = new PillButton("Sign in", PillButton.Style.WARM);
        login.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JPanel reg = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        reg.setOpaque(false);
        reg.add(Theme.label("New student?", Theme.SMALL, Theme.MUTED));
        JButton register = linkButton("Create an account");
        reg.add(register);

        JLabel demo = Theme.label("<html><center>Demo logins<br><b>student</b> / " + DataSeeder.DEMO_STUDENT_PASS
                + " &nbsp;&nbsp; <b>admin</b> / " + DataSeeder.DEFAULT_ADMIN_PASS + "</center></html>", Theme.SMALL, Theme.MUTED);
        demo.setHorizontalAlignment(JLabel.CENTER);
        JPanel demoBox = new RoundPanel(new BorderLayout(), Theme.SOFT).border(null).shadow(false).radius(14);
        demoBox.setBorder(Theme.pad(8, 10));
        demoBox.add(demo);

        JComponent[] items = {hi, sub, u, userField, pw, passField, login, reg, demoBox};
        int[] gaps = {4, 24, 6, 14, 6, 22, 14, 18, 0};
        for (int i = 0; i < items.length; i++) {
            items[i].setAlignmentX(Component.LEFT_ALIGNMENT);
            if (items[i] instanceof TextInput || items[i] instanceof PasswordInput || items[i] == reg || items[i] == demoBox) {
                items[i].setMaximumSize(new Dimension(Integer.MAX_VALUE, items[i].getPreferredSize().height));
            }
            card.add(items[i]);
            if (gaps[i] > 0) card.add(Box.createVerticalStrut(gaps[i]));
        }
        outer.add(card);

        login.addActionListener(e -> doLogin());
        passField.getField().addActionListener(e -> doLogin());
        userField.getField().addActionListener(e -> passField.getField().requestFocusInWindow());
        register.addActionListener(e -> {
            RegisterDialog d = new RegisterDialog(frame, ctx);
            d.setVisible(true);
            if (d.getCreatedUser() != null) {        // ready to sign in: username filled, cursor in the password box
                userField.setText(d.getCreatedUser().getUsername());
                passField.setText("");
                passField.getField().requestFocusInWindow();
            }
        });
        return outer;
    }

    static JButton linkButton(String text) {
        JButton b = new JButton("<html><u>" + text + "</u></html>");
        b.setFont(Theme.SMALL_BOLD);
        b.setForeground(Theme.INDIGO);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void doLogin() {
        UiUtil.run(this, () -> {
            User u = ctx.auth().login(userField.getText(), new String(passField.getPassword()));
            ctx.setCurrentUser(u);
            frame.showDashboard();
        });
        passField.setText("");
    }

    /** Coloured circle with a short symbol, used in the feature list. */
    private static final class Dot extends JComponent {
        private final String text;
        private final Color color;

        Dot(String text, Color color) {
            this.text = text;
            this.color = color;
            setPreferredSize(new Dimension(34, 34));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(color);
            g.fillOval(0, 0, 34, 34);
            g.setColor(Color.WHITE);
            g.setFont(text.length() > 2 ? Theme.SMALL_BOLD : Theme.BODY_BOLD);
            java.awt.FontMetrics fm = g.getFontMetrics();
            g.drawString(text, (34 - fm.stringWidth(text)) / 2, (34 - fm.getHeight()) / 2 + fm.getAscent());
            g.dispose();
        }
    }
}
