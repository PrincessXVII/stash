package dev.stash.domain;

import java.util.List;

public interface StashRepository extends AutoCloseable {
    void replace(ContainerSnapshot snapshot);

    void forget(String locationKey);

    List<SearchHit> find(String displayPattern, String itemPattern, int limit);

    void pruneOldest(int maxContainers);

    @Override
    void close();
}
