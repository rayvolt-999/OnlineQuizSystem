package com.quiz.ui;

import com.quiz.model.Subject;
import com.quiz.service.AdminService;
import com.quiz.ui.components.PillButton;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;

/** Add / edit a subject: name, badge text, colour (preset swatches or full colour chooser) and description. */
public class SubjectDialog extends JDialog {

    private static final String[] PRESETS = {"#2563EB", "#F97316", "#0D9488", "#DB2777", "#CA8A04", "#DC2626", "#0891B2", "#7C3AED", "#16A34A"};

    private final JTextField nameField = new JTextField(26);
    private final JTextField codeField = new JTextField(4);
    private final JTextField descField = new JTextField(26);
    private final JButton colorButton = new JButton();
    private String color = PRESETS[0];
    private boolean saved;

    public SubjectDialog(Window owner, AdminService admin, Subject existing) {
        super(owner, existing == null ? "Add Subject" : "Edit Subject", ModalityType.APPLICATION_MODAL);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Theme.CARD);
        form.setBorder(Theme.pad(18, 20));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        JPanel swatches = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        swatches.setOpaque(false);
        for (String hex : PRESETS) {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(24, 24));
            b.setBackground(Color.decode(hex));
            b.setContentAreaFilled(false);
            b.setOpaque(true);
            b.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
            b.setToolTipText(hex);
            b.addActionListener(e -> setColor(hex));
            swatches.add(b);
        }
        colorButton.setText("More…");
        colorButton.addActionListener(e -> {
            Color c = JColorChooser.showDialog(this, "Subject colour", Color.decode(color));
            if (c != null) setColor(String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue()));
        });
        swatches.add(colorButton);

        Object[][] rows = {
            {"Subject name", nameField},
            {"Badge text (1-4 chars)", codeField},
            {"Colour", swatches},
            {"Description", descField},
        };
        for (int i = 0; i < rows.length; i++) {
            g.gridy = i;
            g.gridx = 0;
            form.add(Theme.label((String) rows[i][0], Theme.SMALL_BOLD, Theme.TEXT), g);
            g.gridx = 1;
            form.add((java.awt.Component) rows[i][1], g);
        }

        if (existing != null) {
            nameField.setText(existing.getName());
            codeField.setText(existing.getShortCode());
            descField.setText(existing.getDescription());
            setColor(existing.getColor() == null ? PRESETS[0] : existing.getColor());
        } else {
            setColor(PRESETS[0]);
        }

        PillButton save = new PillButton("Save", PillButton.Style.WARM);
        PillButton cancel = new PillButton("Cancel", PillButton.Style.SECONDARY);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttons.setBackground(Theme.CARD);
        buttons.add(cancel);
        buttons.add(save);
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(save);

        cancel.addActionListener(e -> dispose());
        save.addActionListener(e -> UiUtil.run(this, () -> {
            Subject s = new Subject(existing == null ? 0 : existing.getId(), nameField.getText(), codeField.getText(),
                    descField.getText().trim(), color);
            admin.saveSubject(s);
            saved = true;
            dispose();
        }));
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void setColor(String hex) {
        color = hex;
        colorButton.setForeground(Color.decode(hex));
        colorButton.setText("■ " + hex);
    }

    public boolean isSaved() {
        return saved;
    }
}
