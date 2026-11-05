package com.quiz.exception;

/** Thrown when user input (a form, an imported row, a quiz setting) is invalid. */
public class ValidationException extends QuizException {

    public ValidationException(String message) {
        super(message);
    }
}
