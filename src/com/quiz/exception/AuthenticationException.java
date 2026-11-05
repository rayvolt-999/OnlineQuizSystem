package com.quiz.exception;

/** Thrown when a login or registration attempt fails. */
public class AuthenticationException extends QuizException {

    public AuthenticationException(String message) {
        super(message);
    }
}
