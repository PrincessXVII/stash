package dev.stash.domain;

public final class LocationKeys {
    public static final String ENDER = "ender";
    private static final String BLOCK_PREFIX = "block|";
    private static final String ENTITY_PREFIX = "entity|";

    private LocationKeys() {
    }

    public static String block(String dimensionId, int x, int y, int z) {
        return BLOCK_PREFIX + dimensionId + "|" + x + "|" + y + "|" + z;
    }

    public static String entity(String uuid) {
        return ENTITY_PREFIX + uuid;
    }
}
