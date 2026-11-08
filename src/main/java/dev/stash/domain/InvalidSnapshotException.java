package dev.stash.domain;

public final class InvalidSnapshotException extends IllegalArgumentException {
    public InvalidSnapshotException(String message) {
        super(message);
    }
}
