package com.quiz.ui.components;

import com.quiz.ui.Theme;

import javax.swing.JButton;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Rounded, custom-painted button with hover and pressed states. Works the same under any look-and-feel. */
public class PillButton extends JButton {

    public enum Style { PRIMARY, WARM, SECONDARY, GHOST, DANGER, SUCCESS }

    private final Style style;
    private Color base;
    private boolean hover;
    private boolean mouseFocus;   // focus came from a click: no ring (ring only for keyboard / Tab focus)

    public PillButton(String text, Style style) {
        super(text);
        this.style = style;
        switch (style) {
            case WARM: base = Theme.ORANGE; break;
            case DANGER: base = Theme.ROSE; break;
            case SUCCESS: base = Theme.CORRECT; break;
            case SECONDARY: case GHOST: base = Color.WHITE; break;
            default: base = Theme.INDIGO;
        }
        setFont(style == Style.SECONDARY || style == Style.GHOST ? Theme.BODY : Theme.BODY_BOLD);
        setForeground(style == Style.SECONDARY ? Theme.TEXT : Color.WHITE);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(Theme.pad(9, 20));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
            @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            @Override public void mousePressed(MouseEvent e) { mouseFocus = true; }
        });
        addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) { repaint(); }
            @Override public void focusLost(java.awt.event.FocusEvent e) { mouseFocus = false; repaint(); }
        });
    }

    /** Custom base colour (for subject-coloured buttons). */
    public PillButton color(Color c) {
        this.base = c;
        repaint();
        return this;
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int w = fm.stringWidth(getText()) + 44 + (getIcon() == null ? 0 : getIcon().getIconWidth() + 8);
        return new Dimension(Math.max(w, 90), Math.max(38, fm.getHeight() + 18));
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        int arc = h;
        boolean pressed = getModel().isPressed();
        boolean enabled = isEnabled();

        switch (style) {
            case SECONDARY: {
                g.setColor(!enabled ? Theme.SKIPPED_BG : pressed ? new Color(0xE2E8F0) : hover ? new Color(0xF1F5F9) : Color.WHITE);
                g.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
                g.setColor(hover && enabled ? Theme.INDIGO : Theme.BORDER);
                g.setStroke(new BasicStroke(1.2f));
                g.drawRoundRect(0, 0, w - 2, h - 2, arc, arc);
                break;
            }
            case GHOST: {
                g.setColor(new Color(255, 255, 255, pressed ? 90 : hover ? 60 : 36));
                g.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
                g.setColor(new Color(255, 255, 255, 120));
                g.drawRoundRect(0, 0, w - 2, h - 2, arc, arc);
                break;
            }
            default: {
                Color c = !enabled ? Theme.SKIPPED : pressed ? Theme.shade(base, 0.18) : hover ? Theme.shade(base, 0.08) : base;
                Color c2 = style == Style.WARM && enabled ? Theme.ROSE : Theme.shade(c, 0.12);
                if (style == Style.PRIMARY && enabled) c2 = Theme.shade(Theme.SKY, pressed ? 0.2 : hover ? 0.1 : 0);
                g.setPaint(new GradientPaint(0, 0, c, w, h, c2));
                g.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
            }
        }

        if (isFocusOwner() && enabled && !mouseFocus) {           // keyboard users can see which button Space/Enter will press
            g.setColor(style == Style.GHOST ? new Color(255, 255, 255, 200) : Theme.alpha(Theme.INDIGO, 140));
            g.setStroke(new BasicStroke(2f));
            g.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);
        }

        Font f = getFont();
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics();
        String text = getText();
        int iconW = getIcon() == null ? 0 : getIcon().getIconWidth() + 8;
        int tx = (w - fm.stringWidth(text) - iconW) / 2;
        int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
        if (getIcon() != null) {
            getIcon().paintIcon(this, g, tx, (h - getIcon().getIconHeight()) / 2);
            tx += iconW;
        }
        g.setColor(enabled ? getForeground() : (style == Style.SECONDARY ? Theme.SKIPPED : Color.WHITE));
        g.drawString(text, tx, ty);
        g.dispose();
    }
}
