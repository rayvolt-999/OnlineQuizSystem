package com.quiz.ui.components;

import com.quiz.ui.Theme;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Rounded-square subject badge with a short code (e.g. "DS") on a gradient of the subject colour. */
public class Badge extends JComponent {

    private final String text;
    private final Color color;
    private final int size;

    public Badge(String text, Color color, int size) {
        this.text = text;
        this.color = color;
        this.size = size;
        setPreferredSize(new Dimension(size, size));
        setMinimumSize(new Dimension(size, size));
        setMaximumSize(new Dimension(size, size));
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, Theme.tint(color, 0.15), size, size, Theme.shade(color, 0.15)));
        g.fillRoundRect(0, 0, size, size, size / 2, size / 2);
        g.setColor(new Color(255, 255, 255, 40));
        g.fillOval(size / 2, -size / 4, size, size * 3 / 4);
        g.setColor(Color.WHITE);
        float fs = text.length() <= 2 ? size * 0.38f : size * 0.3f;
        g.setFont(new Font(Theme.FONT, Font.BOLD, Math.round(fs)));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (size - fm.stringWidth(text)) / 2, (size - fm.getHeight()) / 2 + fm.getAscent());
        g.dispose();
    }
}
