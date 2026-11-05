package com.quiz.dao;

import com.quiz.exception.DataAccessException;
import com.quiz.model.User;

import java.util.List;
import java.util.Optional;

/** Persistence contract for user accounts. Passwords are only ever handled as salted hashes. */
public interface UserDAO {

    /** Returns the user if the username exists and the password matches. */
    Optional<User> authenticate(String username, String password) throws DataAccessException;

    boolean exists(String username) throws DataAccessException;

    User create(String username, String fullName, String password, User.Role role) throws DataAccessException;

    List<User> findAll() throws DataAccessException;

    int count() throws DataAccessException;
}
