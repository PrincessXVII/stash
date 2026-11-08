package dev.stash.domain;

public enum ContainerKind {
    CHEST,
    BARREL,
    SHULKER,
    ENDER,
    HOPPER,
    DISPENSER,
    DROPPER,
    FURNACE,
    BREWING,
    CRAFTER,
    MINECART,
    OTHER;

    public static ContainerKind fromStored(String stored) {
        if (stored == null || stored.isBlank()) {
            return OTHER;
        }
        try {
            return valueOf(stored);
        } catch (IllegalArgumentException exception) {
            return OTHER;
        }
    }
}
