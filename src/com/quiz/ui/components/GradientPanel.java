package com.quiz.ui.components;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

/** Panel painted with a diagonal two-colour gradient plus soft decorative circles. */
public class GradientPanel extends JPanel {

    private Color from;
    private Color to;
    private final boolean decorate;
    private final int radius;

    public GradientPanel(LayoutManager layout, Color from, Color to, boolean decorate) {
        this(layout, from, to, decorate, 0);
    }

    public GradientPanel(LayoutManager layout, Color from, Color to, boolean decorate, int radius) {
        super(layout);
        this.from = from;
        this.to = to;
        this.decorate = decorate;
        this.radius = radius;
        setOpaque(false);
    }

    public void setColors(Color from, Color to) {
        this.from = from;
        this.to = to;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        g.setPaint(new GradientPaint(0, 0, from, w, h, to));
        if (radius > 0) {
            g.fillRoundRect(0, 0, w, h, radius, radius);
            g.setClip(new java.awt.geom.RoundRectangle2D.Float(0, 0, w, h, radius, radius));
        } else {
            g.fillRect(0, 0, w, h);
        }
        if (decorate) {
            g.setColor(new Color(255, 255, 255, 28));
            int d = Math.max(w, h);
            g.fillOval(w - d / 3, -d / 4, d / 2, d / 2);
            g.setColor(new Color(255, 255, 255, 18));
            g.fillOval(-d / 6, h - d / 5, d / 3, d / 3);
            g.fillOval(w / 2, h - d / 8, d / 6, d / 6);
        }
        g.dispose();
        super.paintComponent(g0);
    }
}
