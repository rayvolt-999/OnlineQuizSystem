package com.quiz.ui;

import com.quiz.exception.DataAccessException;
import com.quiz.exception.QuizException;
import com.quiz.exception.ValidationException;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Scrollable;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.LayoutManager;
import java.awt.Rectangle;

/** Helpers: friendly error dialogs instead of stack traces, plus small layout utilities. */
public final class UiUtil {

    /** A UI action that may throw an application exception. */
    @FunctionalInterface
    public interface Action {
        void run() throws QuizException;
    }

    private UiUtil() {
    }

    /** Runs the action and shows any failure in a dialog. Returns true on success. */
    public static boolean run(Component parent, Action action) {
        try {
            action.run();
            return true;
        } catch (ValidationException e) {
            warn(parent, e.getMessage());
        } catch (DataAccessException e) {
            error(parent, "Data error", e.getMessage());
        } catch (QuizException e) {
            warn(parent, e.getMessage());
        } catch (RuntimeException e) {
            e.printStackTrace();
            error(parent, "Unexpected error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return false;
    }

    public static void info(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void warn(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Please check", JOptionPane.WARNING_MESSAGE);
    }

    public static void error(Component parent, String title, String msg) {
        JOptionPane.showMessageDialog(parent, msg, title, JOptionPane.ERROR_MESSAGE);
    }

    public static boolean confirm(Component parent, String msg, String title) {
        return JOptionPane.showConfirmDialog(parent, msg, title, JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    /** Wraps plain text in HTML so a JLabel word-wraps it at the given width. */
    public static String wrap(String text, int widthPx) {
        return "<html><div style='width:" + widthPx + "px'>" + escape(text) + "</div></html>";
    }

    public static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
    }

    /** Read-only, transparent, word-wrapping text block that reflows with the window width. */
    public static JTextArea textBlock(String text, Font font, Color color) {
        JTextArea a = new JTextArea(text) {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                getUI().paint(g, this);   // skip the look-and-feel background so the text sits on the card colour
            }
        };
        a.setFont(font);
        a.setForeground(color);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setEditable(false);
        a.setFocusable(false);
        a.setOpaque(false);
        a.setBorder(BorderFactory.createEmptyBorder());
        return a;
    }

    /** Borderless, transparent scroll pane with sensible scroll speed. */
    public static JScrollPane scroll(Component c) {
        JScrollPane s = new JScrollPane(c);
        s.setBorder(BorderFactory.createEmptyBorder());
        s.setViewportBorder(null);   // Nimbus adds a grey viewport border by default
        s.setOpaque(false);
        s.getViewport().setOpaque(false);
        s.getVerticalScrollBar().setUnitIncrement(18);
        return s;
    }

    /** A panel that always matches the viewport width, so its content wraps instead of scrolling sideways. */
    public static JPanel widthTracking(LayoutManager layout) {
        return new WidthTrackingPanel(layout);
    }

    private static final class WidthTrackingPanel extends JPanel implements Scrollable {
        WidthTrackingPanel(LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 18; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return Math.max(40, r.height - 40); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    /**
     * Left-aligns a component for vertical BoxLayout stacks. (Stacks used in this UI always live at their
     * preferred height, so wrapped text is never stretched or clipped.)
     */
    public static <T extends JComponent> T capHeight(T c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    /** Re-validates once more after the first layout, so word-wrapped text gets its final height. */
    public static void relayoutLater(JComponent c) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            c.revalidate();
            c.repaint();
        });
    }
}
