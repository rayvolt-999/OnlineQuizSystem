package com.quiz.dao.jdbc;

import com.quiz.dao.UserDAO;
import com.quiz.db.DatabaseManager;
import com.quiz.exception.DataAccessException;
import com.quiz.model.User;
import com.quiz.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC (SQLite) implementation of {@link UserDAO}. */
public class JdbcUserDAO implements UserDAO {

    private final DatabaseManager db;

    public JdbcUserDAO(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public Optional<User> authenticate(String username, String password) throws DataAccessException {
        String sql = "SELECT id, username, full_name, password_hash, salt, role FROM users WHERE username = ? COLLATE NOCASE";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && PasswordUtil.matches(password, rs.getString("salt"), rs.getString("password_hash"))) {
                    return Optional.of(map(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Login failed due to a database error: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(String username) throws DataAccessException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM users WHERE username = ? COLLATE NOCASE")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not check username: " + e.getMessage(), e);
        }
    }

    @Override
    public User create(String username, String fullName, String password, User.Role role) throws DataAccessException {
        String salt = PasswordUtil.newSalt();
        String sql = "INSERT INTO users(username, full_name, password_hash, salt, role) VALUES (?,?,?,?,?)";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, fullName);
            ps.setString(3, PasswordUtil.hash(password, salt));
            ps.setString(4, salt);
            ps.setString(5, role.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                int id = keys.next() ? keys.getInt(1) : 0;
                return new User(id, username, fullName, role);
            }
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                throw new DataAccessException("Username '" + username + "' is already taken", e);
            }
            throw new DataAccessException("Could not create user: " + e.getMessage(), e);
        }
    }

    @Override
    public List<User> findAll() throws DataAccessException {
        List<User> list = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id, username, full_name, role FROM users ORDER BY username");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new DataAccessException("Could not load users: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public int count() throws DataAccessException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM users");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not count users: " + e.getMessage(), e);
        }
    }

    private static User map(ResultSet rs) throws SQLException {
        return new User(rs.getInt("id"), rs.getString("username"), rs.getString("full_name"),
                User.Role.valueOf(rs.getString("role")));
    }
}
