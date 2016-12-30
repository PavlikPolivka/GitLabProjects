package com.ppolivka.gitlabprojects.exception;


import org.jetbrains.annotations.NonNls;

public class GitLabException extends RuntimeException {

    public GitLabException() {
    }

    public GitLabException(@NonNls String message) {
        super(message);
    }

    public GitLabException(String message, Throwable cause) {
        super(message, cause);
    }

    public GitLabException(Throwable cause) {
        super(cause);
    }

    public GitLabException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
