package com.quiz.ui.components;

import com.quiz.ui.Theme;
import com.quiz.ui.UiUtil;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Clickable answer option: a letter bubble plus the option text, with hover and selected states. */
public class OptionCard extends JPanel {

    private final char letter;
    private final JLabel text = new JLabel();
    private boolean selected;
    private boolean hover;
    private Runnable onClick;

    public OptionCard(char letter) {
        super(new BorderLayout(12, 0));
        this.letter = letter;
        setOpaque(false);
        setBorder(Theme.pad(10, 14));
        JPanel bubbleSpace = new JPanel();
        bubbleSpace.setOpaque(false);
        bubbleSpace.setPreferredSize(new Dimension(30, 30));
        add(bubbleSpace, BorderLayout.WEST);
        text.setFont(Theme.BODY);
        text.setForeground(Theme.TEXT);
        add(text, BorderLayout.CENTER);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
            @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            @Override public void mouseClicked(MouseEvent e) { if (onClick != null && isEnabled()) onClick.run(); }
        });
    }

    public void setOnClick(Runnable r) { this.onClick = r; }

    /** Option text is shown in monospace when it looks like program output / code. */
    public void setOption(String value, boolean monospace, int wrapWidth) {
        text.setFont(monospace ? Theme.CODE : Theme.BODY);
        text.setText(UiUtil.wrap(value, wrapWidth));
    }

    public void setSelected(boolean s) {
        this.selected = s;
        text.setFont(text.getFont().deriveFont(s ? Font.BOLD : Font.PLAIN));
        repaint();
    }

    public boolean isSelected() { return selected; }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        Color fill = selected ? new Color(0xEEF2FF) : hover ? new Color(0xF8FAFC) : Color.WHITE;
        Color stroke = selected ? Theme.INDIGO : hover ? new Color(0xA5B4FC) : Theme.BORDER;
        g.setColor(fill);
        g.fillRoundRect(0, 0, w - 1, h - 1, 16, 16);
        g.setColor(stroke);
        g.setStroke(new BasicStroke(selected ? 2f : 1.2f));
        g.drawRoundRect(1, 1, w - 3, h - 3, 16, 16);

        int d = 28;
        int bx = 14;
        int by = (h - d) / 2;
        g.setColor(selected ? Theme.INDIGO : Theme.SOFT);
        g.fillOval(bx, by, d, d);
        g.setColor(selected ? Color.WHITE : Theme.INDIGO);
        g.setFont(Theme.BODY_BOLD);
        FontMetrics fm = g.getFontMetrics();
        String s = String.valueOf(letter);
        g.drawString(s, bx + (d - fm.stringWidth(s)) / 2, by + (d - fm.getHeight()) / 2 + fm.getAscent());
        g.dispose();
        super.paintComponent(g0);
    }
}
