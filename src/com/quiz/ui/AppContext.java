package com.quiz.ui;

import com.quiz.dao.DAOFactory;
import com.quiz.model.User;
import com.quiz.service.AdminService;
import com.quiz.service.AuthService;
import com.quiz.service.QuizService;

/** Shared services plus the logged-in user, handed to every screen. */
public class AppContext {

    private final AuthService auth;
    private final QuizService quiz;
    private final AdminService admin;
    private User currentUser;

    public AppContext(DAOFactory dao) {
        this.auth = new AuthService(dao.users());
        this.quiz = new QuizService(dao);
        this.admin = new AdminService(dao);
    }

    public AuthService auth() { return auth; }
    public QuizService quiz() { return quiz; }
    public AdminService admin() { return admin; }

    public User getCurrentUser() { return currentUser; }
    public void setCurrentUser(User currentUser) { this.currentUser = currentUser; }
}
