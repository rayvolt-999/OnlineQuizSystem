package com.quiz.exception;

/** Base checked exception for every application-level failure in the quiz system. */
public class QuizException extends Exception {

    public QuizException(String message) {
        super(message);
    }

    public QuizException(String message, Throwable cause) {
        super(message, cause);
    }
}
