package dev.stash.domain;

import java.util.List;

public sealed interface SearchOutcome {
    record NeedQuery() implements SearchOutcome {
    }

    record None() implements SearchOutcome {
    }

    record Hits(List<SearchHit> hits) implements SearchOutcome {
    }
}
