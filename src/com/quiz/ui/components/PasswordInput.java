package com.quiz.ui.components;

import com.quiz.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JToggleButton;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

/**
 * Password field with an eye button that shows / hides the typed password,
 * like the login boxes on most websites.
 */
public class PasswordInput extends JPanel {

    private final JPasswordField field = new JPasswordField();
    private final JToggleButton eye = new JToggleButton();
    private final char echo;

    public PasswordInput() {
        super(new BorderLayout());
        setOpaque(false);
        setBorder(new RoundBorder(14, new Insets(4, 10, 4, 4)));
        echo = '•';
        field.setEchoChar(echo);
        field.setFont(Theme.BODY);
        field.setBorder(BorderFactory.createEmptyBorder(6, 2, 6, 2));
        field.setOpaque(false);
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { repaint(); }
            @Override public void focusLost(FocusEvent e) { repaint(); }
        });

        eye.setIcon(new EyeIcon(false));
        eye.setSelectedIcon(new EyeIcon(true));
        eye.setContentAreaFilled(false);
        eye.setBorderPainted(false);
        eye.setFocusPainted(false);
        eye.setFocusable(false);
        eye.setOpaque(false);
        eye.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        eye.setToolTipText("Show password");
        eye.setPreferredSize(new Dimension(38, 30));
        eye.addActionListener(e -> {
            boolean show = eye.isSelected();
            field.setEchoChar(show ? (char) 0 : echo);
            eye.setToolTipText(show ? "Hide password" : "Show password");
            field.requestFocusInWindow();
        });

        add(field, BorderLayout.CENTER);
        add(eye, BorderLayout.EAST);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 14, 14);
        g.dispose();
        super.paintComponent(g0);
    }

    public JPasswordField getField() { return field; }
    public char[] getPassword() { return field.getPassword(); }
    public void setText(String s) { field.setText(s); }
    public boolean isRevealed() { return eye.isSelected(); }
    public void toggleReveal() { eye.doClick(); }
    boolean fieldHasFocus() { return field.hasFocus(); }

    /** Eye outline; with a slash when the password is visible ("click to hide"). */
    static final class EyeIcon implements Icon {
        private final boolean open;

        EyeIcon(boolean open) { this.open = open; }

        @Override public int getIconWidth() { return 22; }
        @Override public int getIconHeight() { return 16; }

        @Override
        public void paintIcon(Component c, Graphics g0, int x, int y) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.translate(x, y);
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(open ? Theme.INDIGO : Theme.MUTED);
            Path2D eye = new Path2D.Float();
            eye.moveTo(1, 8);
            eye.quadTo(11, -3, 21, 8);
            eye.quadTo(11, 19, 1, 8);
            g.draw(eye);
            g.fill(new Ellipse2D.Float(7.5f, 4.5f, 7, 7));
            if (open) {
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(3, 15, 19, 1);
                g.setColor(Theme.INDIGO);
                g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(3, 15, 19, 1);
            }
            g.dispose();
        }
    }
}
