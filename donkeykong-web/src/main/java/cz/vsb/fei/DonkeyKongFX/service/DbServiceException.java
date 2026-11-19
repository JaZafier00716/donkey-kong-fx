package cz.vsb.fei.DonkeyKongFX.service;

import org.springframework.http.HttpStatus;

/**
 * The database service could not fulfil a request.
 * {@link #getStatus()} is what this web service should answer its own caller with.
 */
public class DbServiceException extends RuntimeException {
    private final HttpStatus status;

    public DbServiceException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
