package com.quiz.ui.components;

import com.quiz.ui.Theme;

import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Small rounded label ("pill"), e.g. a difficulty tag or status. */
public class Chip extends JLabel {

    private Color fill;

    public Chip(String text, Color fill, Color fg) {
        super(text);
        this.fill = fill;
        setForeground(fg);
        setFont(Theme.SMALL_BOLD);
        setBorder(Theme.pad(4, 11));
        setOpaque(false);
    }

    /** Light background + strong text of the same hue. */
    public static Chip soft(String text, Color c) {
        return new Chip(text, Theme.tint(c, 0.85), Theme.shade(c, 0.25));
    }

    public void setFill(Color fill) {
        this.fill = fill;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(fill);
        g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        g.dispose();
        super.paintComponent(g0);
    }
}
