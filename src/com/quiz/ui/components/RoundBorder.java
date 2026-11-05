package com.quiz.ui.components;

import com.quiz.ui.Theme;

import javax.swing.border.AbstractBorder;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

/** Rounded outline used for text fields; highlights in the accent colour while the field has focus. */
public class RoundBorder extends AbstractBorder {

    private final int radius;
    private final Insets insets;

    public RoundBorder(int radius, Insets insets) {
        this.radius = radius;
        this.insets = insets;
    }

    @Override
    public void paintBorder(Component c, Graphics g0, int x, int y, int w, int h) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        boolean focus = c.hasFocus()
                || (c instanceof PasswordInput && ((PasswordInput) c).fieldHasFocus())
                || (c instanceof TextInput && ((TextInput) c).getField().hasFocus());
        g.setColor(focus ? Theme.INDIGO : Theme.BORDER);
        g.setStroke(new BasicStroke(focus ? 2f : 1.2f));
        g.drawRoundRect(x + 1, y + 1, w - 3, h - 3, radius, radius);
        if (focus) {
            g.setColor(new Color(79, 70, 229, 30));
            g.setStroke(new BasicStroke(4f));
            g.drawRoundRect(x, y, w - 1, h - 1, radius + 2, radius + 2);
        }
        g.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(insets.top, insets.left, insets.bottom, insets.right);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets i) {
        i.set(insets.top, insets.left, insets.bottom, insets.right);
        return i;
    }
}
