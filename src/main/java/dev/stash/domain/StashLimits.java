package dev.stash.domain;

public record StashLimits(int maxContainers, int maxSlotsPerContainer, int maxSearchHits, int queryMaxLength) {
    public static final int DEFAULT_MAX_CONTAINERS = 4_000;
    public static final int DEFAULT_MAX_SLOTS = 256;
    public static final int DEFAULT_MAX_HITS = 40;
    public static final int DEFAULT_QUERY_LENGTH = 64;
    public static final int SEARCH_CANDIDATES = 200;

    public static final int MIN_CONTAINERS = 10;
    public static final int MAX_CONTAINERS_CAP = 50_000;
    public static final int MIN_SLOTS = 9;
    public static final int MAX_SLOTS_CAP = 512;
    public static final int MIN_HITS = 5;
    public static final int MAX_HITS_CAP = 200;
    public static final int MIN_QUERY_LENGTH = 8;
    public static final int MAX_QUERY_LENGTH_CAP = 128;

    public StashLimits {
        maxContainers = clamp(maxContainers, MIN_CONTAINERS, MAX_CONTAINERS_CAP);
        maxSlotsPerContainer = clamp(maxSlotsPerContainer, MIN_SLOTS, MAX_SLOTS_CAP);
        maxSearchHits = clamp(maxSearchHits, MIN_HITS, MAX_HITS_CAP);
        queryMaxLength = clamp(queryMaxLength, MIN_QUERY_LENGTH, MAX_QUERY_LENGTH_CAP);
    }

    public static StashLimits defaults() {
        return new StashLimits(DEFAULT_MAX_CONTAINERS, DEFAULT_MAX_SLOTS, DEFAULT_MAX_HITS, DEFAULT_QUERY_LENGTH);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
