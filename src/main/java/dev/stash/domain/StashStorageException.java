package dev.stash.domain;

public final class StashStorageException extends RuntimeException {
    public StashStorageException(String message) {
        super(message);
    }

    public StashStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
