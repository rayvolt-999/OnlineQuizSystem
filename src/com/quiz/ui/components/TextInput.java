package com.quiz.ui.components;

import com.quiz.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/** Rounded text box (a JTextField inside a painted rounded panel). */
public class TextInput extends JPanel {

    private final JTextField field;

    public TextInput(int columns) {
        super(new BorderLayout());
        setOpaque(false);
        setBorder(new RoundBorder(14, new Insets(4, 10, 4, 10)));
        field = new JTextField(columns);
        field.setFont(Theme.BODY);
        field.setOpaque(false);
        field.setBorder(BorderFactory.createEmptyBorder(6, 2, 6, 2));
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { repaint(); }
            @Override public void focusLost(FocusEvent e) { repaint(); }
        });
        add(field, BorderLayout.CENTER);
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

    public JTextField getField() { return field; }
    public String getText() { return field.getText(); }
    public void setText(String s) { field.setText(s); }
}
