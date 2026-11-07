package com.quiz.ui;

import com.quiz.model.User;
import com.quiz.ui.components.PasswordInput;
import com.quiz.ui.components.PillButton;
import com.quiz.ui.components.TextInput;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;

/** Modal dialog for a student to create an account. Both password boxes have a show/hide eye. */
public class RegisterDialog extends JDialog {

    private final TextInput nameField = new TextInput(22);
    private final TextInput userField = new TextInput(22);
    private final PasswordInput passField = new PasswordInput();
    private final PasswordInput confirmField = new PasswordInput();
    private User created;

    public RegisterDialog(JFrame owner, AppContext ctx) {
        super(owner, "Create a student account", true);
        JPanel form = new JPanel();
        form.setBackground(Theme.CARD);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(Theme.pad(22, 26));

        form.add(left(Theme.label("Create your account", Theme.TITLE, Theme.TEXT)));
        form.add(left(Theme.label("Your results and analytics are saved to this account.", Theme.SMALL, Theme.MUTED)));
        form.add(Box.createVerticalStrut(18));
        String[] labels = {"Full name", "Username (3-20 letters, digits, _ or .)", "Password (min. 4 characters)", "Confirm password"};
        JComponent[] fields = {nameField, userField, passField, confirmField};
        for (int i = 0; i < labels.length; i++) {
            form.add(left(Theme.label(labels[i], Theme.SMALL_BOLD, Theme.TEXT)));
            form.add(Box.createVerticalStrut(5));
            fields[i].setMaximumSize(new Dimension(Integer.MAX_VALUE, fields[i].getPreferredSize().height));
            form.add(left(fields[i]));
            form.add(Box.createVerticalStrut(12));
        }

        PillButton create = new PillButton("Register", PillButton.Style.WARM);
        PillButton cancel = new PillButton("Cancel", PillButton.Style.SECONDARY);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        buttons.setBackground(Theme.CARD);
        buttons.add(cancel);
        buttons.add(create);

        getContentPane().setBackground(Theme.CARD);
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(create);

        cancel.addActionListener(e -> dispose());
        create.addActionListener(e -> UiUtil.run(this, () -> {
            User u = ctx.auth().registerStudent(userField.getText(), nameField.getText(),
                    new String(passField.getPassword()), new String(confirmField.getPassword()));
            UiUtil.info(this, "Account created for " + u.getFullName() + ". You can now sign in as '"
                    + u.getUsername() + "'.");
            created = u;
            dispose();
        }));
        setSize(440, 520);
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    /** The new account, or null if the dialog was cancelled. */
    public User getCreatedUser() {
        return created;
    }

    private static JComponent left(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }
}
