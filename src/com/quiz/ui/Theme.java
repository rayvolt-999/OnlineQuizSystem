package com.quiz.ui;

import com.quiz.model.Difficulty;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Central design tokens: a cool base (indigo, sky, teal) with warm accents (orange, amber, rose),
 * plus fonts and small styling helpers so every screen looks consistent.
 */
public final class Theme {

    // ---- cool base ----
    public static final Color INDIGO = new Color(0x4F46E5);
    public static final Color INDIGO_DARK = new Color(0x3730A3);
    public static final Color SKY = new Color(0x0EA5E9);
    public static final Color TEAL = new Color(0x14B8A6);
    public static final Color NAVY = new Color(0x0F172A);

    // ---- warm accents ----
    public static final Color ORANGE = new Color(0xF97316);
    public static final Color AMBER = new Color(0xF59E0B);
    public static final Color ROSE = new Color(0xF43F5E);
    public static final Color AMBER_BG = new Color(0xFFF7E6);

    // ---- neutrals ----
    public static final Color BG = new Color(0xF4F6FB);
    public static final Color CARD = Color.WHITE;
    public static final Color BORDER = new Color(0xE2E8F0);
    public static final Color TEXT = new Color(0x1E293B);
    public static final Color MUTED = new Color(0x64748B);
    public static final Color SOFT = new Color(0xEEF2FF);

    // ---- feedback ----
    public static final Color CORRECT = new Color(0x059669);
    public static final Color CORRECT_BG = new Color(0xDCFCE7);
    public static final Color WRONG = new Color(0xE11D48);
    public static final Color WRONG_BG = new Color(0xFFE4E6);
    public static final Color SKIPPED = new Color(0x94A3B8);
    public static final Color SKIPPED_BG = new Color(0xF1F5F9);
    public static final Color MARKED = AMBER;
    public static final Color INFO_BG = new Color(0xE0F2FE);

    // ---- code block ----
    public static final Color CODE_BG = new Color(0x0F172A);
    public static final Color CODE_FG = new Color(0xE2E8F0);
    public static final Color CODE_GUTTER = new Color(0x64748B);

    public static final String FONT = Font.SANS_SERIF;
    public static final Font DISPLAY = new Font(FONT, Font.BOLD, 30);
    public static final Font TITLE = new Font(FONT, Font.BOLD, 22);
    public static final Font HEADING = new Font(FONT, Font.BOLD, 16);
    public static final Font BODY = new Font(FONT, Font.PLAIN, 14);
    public static final Font BODY_BOLD = new Font(FONT, Font.BOLD, 14);
    public static final Font SMALL = new Font(FONT, Font.PLAIN, 12);
    public static final Font SMALL_BOLD = new Font(FONT, Font.BOLD, 12);
    public static final Font QUESTION = new Font(FONT, Font.PLAIN, 17);
    public static final Font CODE = new Font(Font.MONOSPACED, Font.PLAIN, 14);
    public static final Font TIMER = new Font(Font.MONOSPACED, Font.BOLD, 18);

    private Theme() {
    }

    /** Installs Nimbus (bundled with every JDK) and tints it with our palette. */
    public static void install() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.put("nimbusBase", INDIGO_DARK);
                    UIManager.put("nimbusBlueGrey", new Color(0xCBD5E1));
                    UIManager.put("control", BG);
                    UIManager.put("nimbusSelectionBackground", INDIGO);
                    UIManager.put("nimbusFocus", SKY);
                    UIManager.put("nimbusOrange", ORANGE);
                    UIManager.put("text", TEXT);
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Nimbus look and feel unavailable, using default: " + e.getMessage());
        }
        UIManager.put("defaultFont", BODY);
        UIManager.put("ToolTip.font", SMALL);
    }

    // ------------------------------------------------------------ colours

    public static Color color(Difficulty d) {
        switch (d) {
            case EASY: return TEAL;
            case MEDIUM: return AMBER;
            default: return ROSE;
        }
    }

    public static Color parse(String hex, Color fallback) {
        try {
            return hex == null ? fallback : Color.decode(hex);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Mixes c with white; amount 0 = c, 1 = white. */
    public static Color tint(Color c, double amount) {
        return new Color(
                (int) (c.getRed() + (255 - c.getRed()) * amount),
                (int) (c.getGreen() + (255 - c.getGreen()) * amount),
                (int) (c.getBlue() + (255 - c.getBlue()) * amount));
    }

    /** Mixes c with black; amount 0 = c, 1 = black. */
    public static Color shade(Color c, double amount) {
        return new Color((int) (c.getRed() * (1 - amount)), (int) (c.getGreen() * (1 - amount)), (int) (c.getBlue() * (1 - amount)));
    }

    public static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    // ------------------------------------------------------------ factories

    public static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    public static Border pad(int v, int h) {
        return BorderFactory.createEmptyBorder(v, h, v, h);
    }

    public static <T extends JComponent> T transparent(T c) {
        c.setOpaque(false);
        return c;
    }

    /** Clean table look: coloured header, zebra rows, no grid. */
    public static void styleTable(JTable t) {
        t.setFont(BODY);
        t.setRowHeight(30);
        t.setFillsViewportHeight(true);
        t.setAutoCreateRowSorter(true);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionBackground(new Color(0xE0E7FF));
        t.setSelectionForeground(TEXT);
        t.setBackground(CARD);

        DefaultTableCellRenderer cells = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object v, boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(table, v, sel, false, row, col);
                setBorder(pad(0, 10));
                if (!sel) setBackground(row % 2 == 0 ? CARD : new Color(0xF8FAFC));
                Class<?> cls = table.getColumnClass(col);
                setHorizontalAlignment(Number.class.isAssignableFrom(cls) ? SwingConstants.CENTER : SwingConstants.LEFT);
                return this;
            }
        };
        t.setDefaultRenderer(Object.class, cells);
        t.setDefaultRenderer(Integer.class, cells);
        t.setDefaultRenderer(Double.class, cells);

        JTableHeader h = t.getTableHeader();
        h.setPreferredSize(new Dimension(10, 34));
        h.setReorderingAllowed(false);
        h.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object v, boolean sel, boolean focus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, v, false, false, row, col);
                l.setOpaque(true);
                l.setBackground(SOFT);
                l.setForeground(INDIGO_DARK);
                l.setFont(SMALL_BOLD);
                l.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(0xC7D2FE)), pad(0, 10)));
                Class<?> cls = table.getColumnClass(col);
                l.setHorizontalAlignment(Number.class.isAssignableFrom(cls) ? SwingConstants.CENTER : SwingConstants.LEFT);
                return l;
            }
        });
    }
}
