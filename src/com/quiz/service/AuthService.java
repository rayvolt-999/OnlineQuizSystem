package com.quiz.service;

import com.quiz.dao.UserDAO;
import com.quiz.exception.AuthenticationException;
import com.quiz.exception.DataAccessException;
import com.quiz.exception.ValidationException;
import com.quiz.model.User;

/** Login and student self-registration, with input validation. */
public class AuthService {

    private final UserDAO users;

    public AuthService(UserDAO users) {
        this.users = users;
    }

    public User login(String username, String password) throws AuthenticationException, DataAccessException {
        if (isBlank(username) || password == null || password.isEmpty()) {
            throw new AuthenticationException("Enter both username and password.");
        }
        return users.authenticate(username.trim(), password)
                .orElseThrow(() -> new AuthenticationException("Invalid username or password."));
    }

    public User registerStudent(String username, String fullName, String password, String confirm)
            throws ValidationException, DataAccessException {
        if (isBlank(fullName)) throw new ValidationException("Full name is required.");
        if (isBlank(username)) throw new ValidationException("Username is required.");
        String u = username.trim();
        if (!u.matches("[A-Za-z0-9_.]{3,20}")) {
            throw new ValidationException("Username must be 3-20 characters: letters, digits, '_' or '.'.");
        }
        if (password == null || password.length() < 4) {
            throw new ValidationException("Password must be at least 4 characters.");
        }
        if (!password.equals(confirm)) throw new ValidationException("Passwords do not match.");
        if (users.exists(u)) throw new ValidationException("Username '" + u + "' is already taken.");
        return users.create(u, fullName.trim(), password, User.Role.STUDENT);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
