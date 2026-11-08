package dev.stash.domain;

public record SearchHit(
        String locationKey,
        String dimensionId,
        int x,
        int y,
        int z,
        ContainerKind kind,
        String label,
        String itemId,
        String displayName,
        int count
) {
}
