package dev.stash.domain;

public final class InvalidSlotException extends IllegalArgumentException {
    public InvalidSlotException(String message) {
        super(message);
    }
}
