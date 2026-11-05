package com.quiz.ui.components;

import com.quiz.ui.Theme;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

/** Rounded "card" with an optional soft shadow, border colour and accent strip on the left. */
public class RoundPanel extends JPanel {

    private Color fill;
    private Color border;
    private Color accent;
    private int radius = 18;
    private boolean shadow = true;
    private int borderWidth = 1;

    public RoundPanel() {
        this(new BorderLayout(), Theme.CARD);
    }

    public RoundPanel(LayoutManager layout, Color fill) {
        super(layout);
        this.fill = fill;
        this.border = Theme.BORDER;
        setOpaque(false);
        setBorder(Theme.pad(16, 18));
    }

    public RoundPanel fill(Color c) { this.fill = c; repaint(); return this; }
    public RoundPanel border(Color c) { this.border = c; repaint(); return this; }
    public RoundPanel border(Color c, int width) { this.border = c; this.borderWidth = width; repaint(); return this; }
    public RoundPanel accent(Color c) { this.accent = c; repaint(); return this; }
    public RoundPanel radius(int r) { this.radius = r; repaint(); return this; }
    public RoundPanel shadow(boolean s) { this.shadow = s; repaint(); return this; }

    public Color getFill() { return fill; }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        int s = shadow ? 3 : 0;
        if (shadow) {
            for (int i = 0; i < 3; i++) {
                g.setColor(new Color(15, 23, 42, 10 - i * 3));
                g.fillRoundRect(i, i + 2, w - i * 2, h - i * 2 - 1, radius + 4, radius + 4);
            }
        }
        g.setColor(fill);
        g.fillRoundRect(0, 0, w - 1, h - s - 1, radius, radius);
        if (accent != null) {
            g.setClip(0, 0, 6, h);
            g.setColor(accent);
            g.fillRoundRect(0, 0, w - 1, h - s - 1, radius, radius);
            g.setClip(null);
        }
        if (border != null) {
            g.setColor(border);
            g.setStroke(new BasicStroke(borderWidth));
            float o = borderWidth / 2f;
            g.drawRoundRect((int) o, (int) o, w - 1 - borderWidth, h - s - 1 - borderWidth, radius, radius);
        }
        g.dispose();
        super.paintComponent(g0);
    }
}
