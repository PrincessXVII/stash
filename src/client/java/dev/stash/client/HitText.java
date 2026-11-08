package dev.stash.client;

import dev.stash.domain.ContainerKind;
import dev.stash.domain.SearchHit;
import net.minecraft.network.chat.Component;

final class HitText {
    private static final String KIND_PREFIX = "stash.kind.";
    private static final String DIMENSION_PREFIX = "stash.dim.";

    private HitText() {
    }

    static String line(SearchHit hit) {
        String place = hit.label().isBlank() ? kind(hit.kind()) : hit.label();
        return hit.displayName()
                + " ×" + hit.count()
                + " — " + place
                + ", " + hit.x() + " " + hit.y() + " " + hit.z()
                + ", " + dimension(hit.dimensionId());
    }

    static String coordinates(SearchHit hit) {
        return hit.x() + " " + hit.y() + " " + hit.z();
    }

    private static String kind(ContainerKind kind) {
        return translated(KIND_PREFIX + kind.name().toLowerCase());
    }

    private static String dimension(String dimensionId) {
        String key = DIMENSION_PREFIX + dimensionId.replace(':', '.');
        String translated = translated(key);
        return translated.equals(key) ? dimensionId : translated;
    }

    private static String translated(String key) {
        return Component.translatable(key).getString();
    }
}
