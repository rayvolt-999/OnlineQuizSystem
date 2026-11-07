package com.quiz.ui.components;

import com.quiz.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Dark, monospaced code viewer with line numbers and a language tag — used for "predict the output" questions. */
public class CodeBlock extends JPanel {

    public CodeBlock(String code, String lang, int maxVisibleLines) {
        this(code, lang, maxVisibleLines, true);
    }

    /**
     * @param selectable false during a test: the code cannot be focused or copied, so clicking it never
     *                   steals the keyboard shortcuts (arrow keys would otherwise move a hidden caret)
     */
    public CodeBlock(String code, String lang, int maxVisibleLines, boolean selectable) {
        super(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(8, 0, 10, 0));

        String[] lines = code.split("\n", -1);
        StringBuilder nums = new StringBuilder();
        for (int i = 1; i <= lines.length; i++) nums.append(i).append(i < lines.length ? "\n" : "");

        JTextArea area = new JTextArea(code.replace("\t", "    "));
        area.setEditable(false);
        area.setFocusable(selectable);
        if (!selectable) area.setHighlighter(null);
        area.setFont(Theme.CODE);
        area.setForeground(Theme.CODE_FG);
        area.setBackground(Theme.CODE_BG);
        area.setCaretColor(Theme.CODE_FG);
        area.setSelectionColor(new Color(0x334155));
        area.setSelectedTextColor(Color.WHITE);
        area.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 14));

        JTextArea gutter = new JTextArea(nums.toString());
        gutter.setEditable(false);
        gutter.setFocusable(false);
        gutter.setFont(Theme.CODE);
        gutter.setForeground(Theme.CODE_GUTTER);
        gutter.setBackground(new Color(0x111C33));
        gutter.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 8));

        JScrollPane scroll = new JScrollPane(area);
        scroll.setRowHeaderView(gutter);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.CODE_BG);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        int lineH = area.getFontMetrics(Theme.CODE).getHeight();
        int visible = Math.min(lines.length, maxVisibleLines);
        scroll.setPreferredSize(new Dimension(200, visible * lineH + 24 + (lines.length > maxVisibleLines ? 0 : 4)));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4)) {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setColor(new Color(0x1E293B));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color[] dots = {new Color(0xF87171), new Color(0xFBBF24), new Color(0x34D399)};
                for (int i = 0; i < 3; i++) {
                    g.setColor(dots[i]);
                    g.fillOval(getWidth() - 62 + i * 18, getHeight() / 2 - 5, 10, 10);
                }
                g.dispose();
            }
        };
        JLabel tag = Theme.label(lang == null ? "code" : lang.toLowerCase(), Theme.SMALL_BOLD, new Color(0x93C5FD));
        top.add(tag);

        JPanel frame = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g0) {
                // rounded clip so the dark block has soft corners
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(Theme.CODE_BG);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g.dispose();
            }
        };
        frame.setOpaque(false);
        frame.setBorder(BorderFactory.createLineBorder(new Color(0x1E293B), 1, true));
        frame.add(top, BorderLayout.NORTH);
        frame.add(scroll, BorderLayout.CENTER);
        add(frame, BorderLayout.CENTER);
    }
}
