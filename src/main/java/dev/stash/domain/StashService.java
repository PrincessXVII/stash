package dev.stash.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class StashService implements AutoCloseable {
    private final StashRepository repository;
    private final StashLimits limits;

    public StashService(StashRepository repository, StashLimits limits) {
        this.repository = repository;
        this.limits = limits;
    }

    public void remember(ContainerSnapshot snapshot) {
        List<StoredSlot> slots = snapshot.slots();
        if (slots.size() > limits.maxSlotsPerContainer()) {
            slots = List.copyOf(slots.subList(0, limits.maxSlotsPerContainer()));
            snapshot = new ContainerSnapshot(
                    snapshot.locationKey(),
                    snapshot.dimensionId(),
                    snapshot.x(),
                    snapshot.y(),
                    snapshot.z(),
                    snapshot.kind(),
                    snapshot.label(),
                    slots,
                    snapshot.seenAtMillis()
            );
        }
        repository.replace(snapshot);
        repository.pruneOldest(limits.maxContainers());
    }

    public void forget(String locationKey) {
        if (locationKey == null || locationKey.isBlank()) {
            return;
        }
        repository.forget(locationKey);
    }

    public SearchOutcome search(String rawQuery, SearchOrigin origin) {
        String normalized = QueryText.normalize(rawQuery, limits.queryMaxLength());
        if (normalized.isEmpty()) {
            return new SearchOutcome.NeedQuery();
        }
        List<SearchHit> found = repository.find(
                QueryText.displayPattern(normalized),
                QueryText.itemPattern(normalized),
                StashLimits.SEARCH_CANDIDATES
        );
        if (found.isEmpty()) {
            return new SearchOutcome.None();
        }
        List<SearchHit> ordered = new ArrayList<>(found);
        ordered.sort(comparator(origin));
        if (ordered.size() > limits.maxSearchHits()) {
            ordered = List.copyOf(ordered.subList(0, limits.maxSearchHits()));
        } else {
            ordered = List.copyOf(ordered);
        }
        return new SearchOutcome.Hits(ordered);
    }

    @Override
    public void close() {
        repository.close();
    }

    private static Comparator<SearchHit> comparator(SearchOrigin origin) {
        return Comparator
                .comparing((SearchHit hit) -> origin != null && !hit.dimensionId().equals(origin.dimensionId()))
                .thenComparingLong(hit -> origin == null ? 0 : distanceSquared(hit, origin))
                .thenComparing(SearchHit::displayName);
    }

    private static long distanceSquared(SearchHit hit, SearchOrigin origin) {
        long dx = (long) hit.x() - origin.x();
        long dy = (long) hit.y() - origin.y();
        long dz = (long) hit.z() - origin.z();
        return dx * dx + dy * dy + dz * dz;
    }
}
