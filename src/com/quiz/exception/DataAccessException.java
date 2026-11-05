package com.quiz.exception;

/** Thrown when reading or writing persistent data (database or files) fails. Wraps SQLException / IOException. */
public class DataAccessException extends QuizException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataAccessException(String message) {
        super(message);
    }
}
