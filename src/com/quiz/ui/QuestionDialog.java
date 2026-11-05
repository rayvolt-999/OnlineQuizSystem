package com.quiz.ui;

import com.quiz.model.Difficulty;
import com.quiz.model.Question;
import com.quiz.model.Subject;
import com.quiz.model.Subtopic;
import com.quiz.service.AdminService;
import com.quiz.ui.components.PillButton;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

/** Add / edit form for one MCQ, including an optional code snippet, hint and explanation. */
public class QuestionDialog extends JDialog {

    private static final String[] LANGS = {"", "java", "python", "c", "cpp", "sql", "bash", "text"};

    private final AdminService admin;
    private final JComboBox<Subject> subjectBox;
    private final JComboBox<Subtopic> subtopicBox = new JComboBox<>();
    private final JComboBox<Difficulty> difficultyBox = new JComboBox<>(Difficulty.values());
    private final JTextArea textArea = area(3, false);
    private final JTextArea codeArea = area(8, true);
    private final JComboBox<String> langBox = new JComboBox<>(LANGS);
    private final JTextField[] optionFields = new JTextField[Question.OPTION_COUNT];
    private final JRadioButton[] correctButtons = new JRadioButton[Question.OPTION_COUNT];
    private final JTextArea hintArea = area(2, false);
    private final JTextArea explainArea = area(3, false);
    private boolean saved;

    public QuestionDialog(Window owner, AdminService admin, List<Subject> subjects, Question existing,
                          Subject defaultSubject, Subtopic defaultSubtopic) {
        super(owner, existing == null ? "Add Question" : "Edit Question", ModalityType.APPLICATION_MODAL);
        this.admin = admin;
        subjectBox = new JComboBox<>(subjects.toArray(new Subject[0]));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Theme.CARD);
        form.setBorder(Theme.pad(16, 20));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.anchor = GridBagConstraints.NORTHWEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;

        JPanel where = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        where.setOpaque(false);
        where.add(subjectBox);
        where.add(subtopicBox);
        where.add(Theme.label("Mode", Theme.SMALL_BOLD, Theme.TEXT));
        where.add(difficultyBox);
        addRow(form, g, row++, "Subject / subtopic", where);
        addRow(form, g, row++, "Question", new JScrollPane(textArea));

        JPanel codePanel = new JPanel(new BorderLayout(0, 4));
        codePanel.setOpaque(false);
        JPanel langRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        langRow.setOpaque(false);
        langRow.add(Theme.label("Language", Theme.SMALL, Theme.MUTED));
        langRow.add(langBox);
        langRow.add(Theme.label("Leave the code empty for theory questions.", Theme.SMALL, Theme.MUTED));
        codePanel.add(langRow, BorderLayout.NORTH);
        codeArea.setBackground(Theme.CODE_BG);
        codeArea.setForeground(Theme.CODE_FG);
        codeArea.setCaretColor(Theme.CODE_FG);
        codePanel.add(new JScrollPane(codeArea), BorderLayout.CENTER);
        addRow(form, g, row++, "Code snippet", codePanel);

        ButtonGroup bg = new ButtonGroup();
        for (int i = 0; i < Question.OPTION_COUNT; i++) {
            JPanel opt = new JPanel(new BorderLayout(8, 0));
            opt.setOpaque(false);
            optionFields[i] = new JTextField(40);
            correctButtons[i] = new JRadioButton("Correct");
            correctButtons[i].setOpaque(false);
            bg.add(correctButtons[i]);
            opt.add(optionFields[i], BorderLayout.CENTER);
            opt.add(correctButtons[i], BorderLayout.EAST);
            addRow(form, g, row++, "Option " + Question.label(i), opt);
        }
        addRow(form, g, row++, "Hint (Medium/Hard)", new JScrollPane(hintArea));
        addRow(form, g, row, "Explanation", new JScrollPane(explainArea));

        subjectBox.addActionListener(e -> loadSubtopics(null));
        if (existing != null) {
            for (Subject s : subjects) if (s.getName().equals(existing.getSubjectName())) subjectBox.setSelectedItem(s);
            loadSubtopics(existing.getSubtopicId());
            difficultyBox.setSelectedItem(existing.getDifficulty());
            textArea.setText(existing.getText());
            codeArea.setText(existing.getCode() == null ? "" : existing.getCode());
            langBox.setSelectedItem(existing.getCodeLang() == null ? "" : existing.getCodeLang());
            for (int i = 0; i < Question.OPTION_COUNT; i++) optionFields[i].setText(existing.getOption(i));
            correctButtons[existing.getCorrectIndex()].setSelected(true);
            hintArea.setText(existing.getHint() == null ? "" : existing.getHint());
            explainArea.setText(existing.getExplanation() == null ? "" : existing.getExplanation());
        } else {
            if (defaultSubject != null) {
                for (Subject s : subjects) if (s.getId() == defaultSubject.getId()) subjectBox.setSelectedItem(s);
            }
            loadSubtopics(defaultSubtopic == null ? null : defaultSubtopic.getId());
            difficultyBox.setSelectedItem(Difficulty.EASY);
        }

        PillButton save = new PillButton("Save question", PillButton.Style.WARM);
        PillButton cancel = new PillButton("Cancel", PillButton.Style.SECONDARY);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttons.setBackground(Theme.CARD);
        buttons.add(cancel);
        buttons.add(save);
        JScrollPane sp = new JScrollPane(form);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getVerticalScrollBar().setUnitIncrement(18);
        getContentPane().add(sp, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);

        cancel.addActionListener(e -> dispose());
        save.addActionListener(e -> UiUtil.run(this, () -> {
            Question q = existing == null ? new Question() : existing;
            Subtopic t = (Subtopic) subtopicBox.getSelectedItem();
            String[] opts = new String[Question.OPTION_COUNT];
            int correct = -1;
            for (int i = 0; i < Question.OPTION_COUNT; i++) {
                opts[i] = optionFields[i].getText().trim();
                if (correctButtons[i].isSelected()) correct = i;
            }
            Question c = new Question(q.getId(), t == null ? 0 : t.getId(), (Difficulty) difficultyBox.getSelectedItem(),
                    textArea.getText().trim(), opts, correct);
            c.setCode(stripTrailing(codeArea.getText()));
            c.setCodeLang((String) langBox.getSelectedItem());
            c.setHint(hintArea.getText().trim());
            c.setExplanation(explainArea.getText().trim());
            AdminService.validate(c);   // validate a copy first so a failed save never mutates the original
            admin.saveQuestion(c);
            saved = true;
            dispose();
        }));
        setSize(860, 760);
        setLocationRelativeTo(owner);
    }

    private void loadSubtopics(Integer selectId) {
        subtopicBox.removeAllItems();
        Subject s = (Subject) subjectBox.getSelectedItem();
        if (s == null) return;
        List<Subtopic> list = new ArrayList<>();
        UiUtil.run(this, () -> list.addAll(admin.getSubtopics(s)));
        for (Subtopic t : list) {
            subtopicBox.addItem(t);
            if (selectId != null && t.getId() == selectId) subtopicBox.setSelectedItem(t);
        }
    }

    private static String stripTrailing(String s) {
        return s == null ? null : s.replaceAll("\\s+$", "");
    }

    private static JTextArea area(int rows, boolean code) {
        JTextArea a = new JTextArea(rows, 40);
        a.setFont(code ? Theme.CODE : Theme.BODY);
        a.setLineWrap(!code);
        a.setWrapStyleWord(true);
        a.setTabSize(4);
        a.setBorder(Theme.pad(6, 8));
        return a;
    }

    private static void addRow(JPanel form, GridBagConstraints g, int row, String label, JComponent field) {
        g.gridy = row;
        g.gridx = 0;
        g.weightx = 0;
        form.add(Theme.label(label, Theme.SMALL_BOLD, Theme.TEXT), g);
        g.gridx = 1;
        g.weightx = 1;
        field.setMinimumSize(new Dimension(200, field.getPreferredSize().height));
        form.add(field, g);
    }

    public boolean isSaved() {
        return saved;
    }
}
