package com.quiz;

import com.quiz.dao.DAOFactory;
import com.quiz.db.DataSeeder;
import com.quiz.db.DatabaseManager;
import com.quiz.exception.DataAccessException;
import com.quiz.ui.AppContext;
import com.quiz.ui.MainFrame;
import com.quiz.ui.Theme;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** Application entry point: prepares the database, then opens the main window on the Swing thread. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            try {
                DatabaseManager db = DatabaseManager.getInstance();
                db.createSchema();
                DAOFactory dao = new DAOFactory(db);
                DataSeeder.seedIfEmpty(dao);
                new MainFrame(new AppContext(dao)).setVisible(true);
            } catch (DataAccessException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Could not start the application:\n" + e.getMessage(),
                        "Startup error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}
