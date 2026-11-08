package dev.stash.domain;

import java.util.ArrayList;
import java.util.List;

public record ContainerSnapshot(
        String locationKey,
        String dimensionId,
        int x,
        int y,
        int z,
        ContainerKind kind,
        String label,
        List<StoredSlot> slots,
        long seenAtMillis
) {
    public ContainerSnapshot {
        if (locationKey == null || locationKey.isBlank()) {
            throw new InvalidSnapshotException("Пустой ключ хранилища");
        }
        if (dimensionId == null || dimensionId.isBlank()) {
            throw new InvalidSnapshotException("Не указано измерение");
        }
        if (kind == null) {
            throw new InvalidSnapshotException("Не указан тип хранилища");
        }
        label = Texts.oneLine(label, Texts.MAX_LABEL_LENGTH);
        if (slots == null) {
            slots = List.of();
        } else {
            slots = List.copyOf(new ArrayList<>(slots));
        }
    }
}
