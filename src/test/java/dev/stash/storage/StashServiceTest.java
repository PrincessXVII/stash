package dev.stash.storage;

import dev.stash.domain.ContainerKind;
import dev.stash.domain.ContainerSnapshot;
import dev.stash.domain.LocationKeys;
import dev.stash.domain.SearchHit;
import dev.stash.domain.SearchOrigin;
import dev.stash.domain.SearchOutcome;
import dev.stash.domain.StashLimits;
import dev.stash.domain.StashService;
import dev.stash.domain.StoredSlot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class StashServiceTest {
    private StashService service;

    @BeforeEach
    void setUp() {
        String url = "jdbc:sqlite:file:stash" + System.nanoTime() + "?mode=memory&cache=shared";
        service = new StashService(new SqliteStashRepository(url), StashLimits.defaults());
    }

    @AfterEach
    void tearDown() {
        service.close();
    }

    @Test
    void findsByRussianNameAndItemId() {
        service.remember(chest("home", 10, 64, -3, List.of(
                slot(0, "minecraft:elytra", 1, "Элитры")
        ), 1));

        SearchOutcome.Hits byName = hits(service.search("элитры", origin()));
        assertEquals(1, byName.hits().getFirst().count());
        assertEquals(10, byName.hits().getFirst().x());

        SearchOutcome.Hits byId = hits(service.search("ELYTRA", origin()));
        assertEquals("minecraft:elytra", byId.hits().getFirst().itemId());
    }

    @Test
    void sumsStacksInOneContainer() {
        service.remember(chest("home", 1, 2, 3, List.of(
                slot(0, "minecraft:diamond", 10, "Алмаз"),
                slot(1, "minecraft:diamond", 5, "Алмаз")
        ), 1));
        assertEquals(15, hits(service.search("алмаз", origin())).hits().getFirst().count());
    }

    @Test
    void replacingContentsDropsTheOldItem() {
        service.remember(chest("home", 1, 2, 3, List.of(slot(0, "minecraft:diamond", 1, "Алмаз")), 1));
        service.remember(chest("home", 1, 2, 3, List.of(slot(0, "minecraft:dirt", 4, "Земля")), 2));
        assertInstanceOf(SearchOutcome.None.class, service.search("алмаз", origin()));
        assertEquals(4, hits(service.search("земля", origin())).hits().getFirst().count());
    }

    @Test
    void emptyVisitForgetsTheContainer() {
        service.remember(chest("home", 1, 2, 3, List.of(slot(0, "minecraft:diamond", 1, "Алмаз")), 1));
        service.remember(chest("home", 1, 2, 3, List.of(), 2));
        assertInstanceOf(SearchOutcome.None.class, service.search("алмаз", origin()));
    }

    @Test
    void wildcardQueryDoesNotMatchEverything() {
        service.remember(chest("home", 1, 2, 3, List.of(slot(0, "minecraft:elytra", 1, "Элитры")), 1));
        assertInstanceOf(SearchOutcome.None.class, service.search("%", origin()));
        assertInstanceOf(SearchOutcome.None.class, service.search("_", origin()));
        assertInstanceOf(SearchOutcome.None.class, service.search("'", origin()));
    }

    @Test
    void closerContainerComesFirst() {
        service.remember(chest("far", 100, 64, 0, List.of(slot(0, "minecraft:stone", 1, "Камень")), 1));
        service.remember(chest("near", 2, 64, 0, List.of(slot(0, "minecraft:stone", 1, "Камень")), 2));
        List<SearchHit> hits = hits(service.search("камень", new SearchOrigin("minecraft:overworld", 0, 64, 0))).hits();
        assertEquals(2, hits.getFirst().x());
        assertEquals(100, hits.get(1).x());
    }

    @Test
    void otherDimensionSortsAfterTheCurrentOne() {
        service.remember(chestIn("minecraft:the_nether", "nether", 0, 64, 0, slot(0, "minecraft:stone", 1, "Камень"), 1));
        service.remember(chest("overworld", 50, 64, 0, List.of(slot(0, "minecraft:stone", 1, "Камень")), 2));
        List<SearchHit> hits = hits(service.search("камень", new SearchOrigin("minecraft:overworld", 0, 64, 0))).hits();
        assertEquals("minecraft:overworld", hits.getFirst().dimensionId());
        assertEquals("minecraft:the_nether", hits.get(1).dimensionId());
    }

    @Test
    void pruneDropsTheOldestContainer() {
        int kept = StashLimits.MIN_CONTAINERS;
        StashService small = new StashService(
                new SqliteStashRepository("jdbc:sqlite:file:stashprune" + System.nanoTime() + "?mode=memory&cache=shared"),
                new StashLimits(kept, 27, 10, 32)
        );
        try {
            small.remember(chest("oldest", 0, 1, 1, List.of(slot(0, "minecraft:apple", 1, "Яблоко")), 1));
            for (int i = 1; i < kept; i++) {
                small.remember(chest("keep" + i, i, 1, 1, List.of(slot(0, "minecraft:bread", 1, "Хлеб")), i + 1L));
            }
            small.remember(chest("newest", kept, 1, 1, List.of(slot(0, "minecraft:cookie", 1, "Печенье")), kept + 2L));
            assertInstanceOf(SearchOutcome.None.class, small.search("яблоко", origin()));
            assertEquals(1, hits(small.search("печенье", origin())).hits().size());
        } finally {
            small.close();
        }
    }

    @Test
    void blankQueryAsksForText() {
        assertInstanceOf(SearchOutcome.NeedQuery.class, service.search("   ", origin()));
    }

    @Test
    void enderChestIsOneRecord() {
        service.remember(new ContainerSnapshot(LocationKeys.ENDER, "minecraft:overworld", 1, 70, 1, ContainerKind.ENDER, "Эндер-сундук",
                List.of(slot(0, "minecraft:diamond", 2, "Алмаз")), 1));
        service.remember(new ContainerSnapshot(LocationKeys.ENDER, "minecraft:overworld", 9, 80, 9, ContainerKind.ENDER, "Эндер-сундук",
                List.of(slot(0, "minecraft:emerald", 1, "Изумруд")), 2));
        assertInstanceOf(SearchOutcome.None.class, service.search("алмаз", origin()));
        assertEquals(9, hits(service.search("изумруд", origin())).hits().getFirst().x());
    }

    private static SearchOutcome.Hits hits(SearchOutcome outcome) {
        return assertInstanceOf(SearchOutcome.Hits.class, outcome);
    }

    private static SearchOrigin origin() {
        return new SearchOrigin("minecraft:overworld", 0, 64, 0);
    }

    private static StoredSlot slot(int index, String itemId, int count, String name) {
        return new StoredSlot(index, itemId, count, name, "");
    }

    private static ContainerSnapshot chest(String name, int x, int y, int z, List<StoredSlot> slots, long seenAt) {
        return chestIn("minecraft:overworld", name, x, y, z, slots, seenAt);
    }

    private static ContainerSnapshot chestIn(String dimension, String name, int x, int y, int z, StoredSlot slot, long seenAt) {
        return chestIn(dimension, name, x, y, z, List.of(slot), seenAt);
    }

    private static ContainerSnapshot chestIn(String dimension, String name, int x, int y, int z, List<StoredSlot> slots, long seenAt) {
        return new ContainerSnapshot(LocationKeys.block(dimension, x, y, z), dimension, x, y, z, ContainerKind.CHEST, name, slots, seenAt);
    }
}
