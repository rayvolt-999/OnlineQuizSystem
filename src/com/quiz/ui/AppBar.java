package com.quiz.ui;

import com.quiz.ui.components.GradientPanel;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;

/** Gradient title bar shown at the top of every main screen: title + subtitle on the left, actions on the right. */
public class AppBar extends GradientPanel {

    private final JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

    public AppBar(String title, String subtitle) {
        this(title, subtitle, Theme.INDIGO, Theme.SKY);
    }

    public AppBar(String title, String subtitle, Color from, Color to) {
        super(new BorderLayout(16, 0), from, to, true);
        setBorder(Theme.pad(14, 24));
        JPanel text = new JPanel(new GridLayout(subtitle == null ? 1 : 2, 1, 0, 2));
        text.setOpaque(false);
        JLabel t = Theme.label(title, Theme.TITLE, Color.WHITE);
        text.add(t);
        if (subtitle != null) text.add(Theme.label(subtitle, Theme.SMALL, new Color(255, 255, 255, 220)));
        add(text, BorderLayout.CENTER);
        actions.setOpaque(false);
        add(actions, BorderLayout.EAST);
    }

    public AppBar left(JComponent c) {
        c.setOpaque(false);
        add(c, BorderLayout.WEST);
        return this;
    }

    public AppBar action(JComponent c) {
        actions.add(c);
        return this;
    }
}
