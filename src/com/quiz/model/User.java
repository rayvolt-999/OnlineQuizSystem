package com.quiz.model;

/** A person who can log in: either a STUDENT (takes quizzes) or an ADMIN (manages questions). */
public class User {

    public enum Role { STUDENT, ADMIN }

    private int id;
    private String username;
    private String fullName;
    private Role role;

    public User(int id, String username, String fullName, Role role) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public Role getRole() { return role; }
    public boolean isAdmin() { return role == Role.ADMIN; }

    @Override
    public String toString() {
        return fullName + " (" + username + ")";
    }
}
