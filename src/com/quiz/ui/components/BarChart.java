package com.quiz.ui.components;

import com.quiz.ui.Theme;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

/** Horizontal bar chart (0-100 %) with a label, a coloured bar and the value at the end. */
public class BarChart extends JComponent {

    private static final int ROW = 34;

    private final List<String> labels = new ArrayList<>();
    private final List<Double> values = new ArrayList<>();
    private final List<Color> colors = new ArrayList<>();
    private final List<String> notes = new ArrayList<>();
    private final int labelWidth;

    public BarChart(int labelWidth) {
        this.labelWidth = labelWidth;
    }

    public void add(String label, double percent, Color color, String note) {
        labels.add(label);
        values.add(Math.max(0, Math.min(100, percent)));
        colors.add(color);
        notes.add(note);
        setPreferredSize(new Dimension(300, labels.size() * ROW + 8));
        revalidate();
        repaint();
    }

    public boolean isEmpty() { return labels.isEmpty(); }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int valueW = 110;
        int barX = labelWidth + 10;
        int barW = Math.max(40, getWidth() - barX - valueW);
        for (int i = 0; i < labels.size(); i++) {
            int y = 4 + i * ROW;
            g.setFont(Theme.SMALL_BOLD);
            g.setColor(Theme.TEXT);
            FontMetrics fm = g.getFontMetrics();
            String lab = labels.get(i);
            while (fm.stringWidth(lab) > labelWidth && lab.length() > 4) lab = lab.substring(0, lab.length() - 2);
            if (!lab.equals(labels.get(i))) lab = lab + "…";
            g.drawString(lab, 0, y + 18);
            g.setColor(Theme.SKIPPED_BG);
            g.fillRoundRect(barX, y + 6, barW, 16, 16, 16);
            int fill = (int) Math.round(barW * values.get(i) / 100.0);
            if (fill > 0) {
                g.setColor(colors.get(i));
                g.fillRoundRect(barX, y + 6, Math.max(fill, 16), 16, 16, 16);
            }
            g.setFont(Theme.SMALL_BOLD);
            g.setColor(Theme.TEXT);
            String v = String.format("%.0f%%", values.get(i));
            g.drawString(v, barX + barW + 10, y + 18);
            if (notes.get(i) != null) {
                g.setFont(Theme.SMALL);
                g.setColor(Theme.MUTED);
                g.drawString(notes.get(i), barX + barW + 10 + g.getFontMetrics(Theme.SMALL_BOLD).stringWidth(v) + 6, y + 18);
            }
        }
        g.dispose();
    }
}
