package com.quiz.ui;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;

/** Custom-painted donut chart of correct / wrong / unanswered with the percentage in the middle. */
public class ScoreChart extends JComponent {

    private final int correct;
    private final int wrong;
    private final int unanswered;
    private final String centerText;
    private final String caption;

    public ScoreChart(int correct, int wrong, int unanswered, String centerText, String caption) {
        this.correct = correct;
        this.wrong = wrong;
        this.unanswered = unanswered;
        this.centerText = centerText;
        this.caption = caption;
        setPreferredSize(new Dimension(230, 230));
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int size = Math.min(getWidth(), getHeight()) - 30;
        int x = (getWidth() - size) / 2;
        int y = (getHeight() - size) / 2;
        float stroke = size * 0.13f;
        g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g.setColor(Theme.SKIPPED_BG);
        g.draw(new Arc2D.Double(x, y, size, size, 0, 360, Arc2D.OPEN));
        int total = correct + wrong + unanswered;
        double start = 90;
        int[] values = {correct, wrong, unanswered};
        Color[] colors = {Theme.TEAL, Theme.ROSE, new Color(0xCBD5E1)};
        if (total > 0) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] == 0) continue;
                double extent = -360.0 * values[i] / total;
                g.setColor(colors[i]);
                g.draw(new Arc2D.Double(x, y, size, size, start, extent, Arc2D.OPEN));
                start += extent;
            }
        }
        g.setColor(Theme.TEXT);
        g.setFont(Theme.DISPLAY.deriveFont(size * 0.2f));
        FontMetrics fm = g.getFontMetrics();
        int cy = getHeight() / 2 + fm.getAscent() / 2 - 10;
        g.drawString(centerText, (getWidth() - fm.stringWidth(centerText)) / 2, cy);
        g.setFont(Theme.SMALL);
        g.setColor(Theme.MUTED);
        fm = g.getFontMetrics();
        g.drawString(caption, (getWidth() - fm.stringWidth(caption)) / 2, cy + 20);
        g.dispose();
    }
}
