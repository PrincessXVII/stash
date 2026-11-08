package dev.stash.storage;

import dev.stash.domain.ContainerKind;
import dev.stash.domain.ContainerSnapshot;
import dev.stash.domain.SearchHit;
import dev.stash.domain.StashRepository;
import dev.stash.domain.StashStorageException;
import dev.stash.domain.StoredSlot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class SqliteStashRepository implements StashRepository {
    static final int BUSY_TIMEOUT_MS = 2_000;

    private static final String REPLACE_CONTAINER = """
            INSERT INTO container(location_key, dimension_id, x, y, z, kind, label, seen_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(location_key) DO UPDATE SET
                dimension_id = excluded.dimension_id,
                x = excluded.x,
                y = excluded.y,
                z = excluded.z,
                kind = excluded.kind,
                label = excluded.label,
                seen_at = excluded.seen_at
            """;

    private static final String DELETE_SLOTS = "DELETE FROM slot WHERE location_key = ?";
    private static final String DELETE_CONTAINER = "DELETE FROM container WHERE location_key = ?";
    private static final String INSERT_SLOT = """
            INSERT INTO slot(location_key, slot_index, item_id, count, display_name, search_name)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
    private static final String FIND = """
            SELECT c.location_key, c.dimension_id, c.x, c.y, c.z, c.kind, c.label,
                   s.item_id, s.display_name, SUM(s.count) AS total
            FROM slot s
            JOIN container c ON c.location_key = s.location_key
            WHERE s.search_name LIKE ? ESCAPE '\\' OR s.item_id LIKE ? ESCAPE '\\'
            GROUP BY c.location_key, s.item_id, s.display_name
            LIMIT ?
            """;
    private static final String PRUNE = """
            DELETE FROM container
            WHERE location_key NOT IN (
                SELECT location_key FROM container ORDER BY seen_at DESC LIMIT ?
            )
            """;

    private final Connection connection;
    private final PreparedStatement replaceContainer;
    private final PreparedStatement deleteSlots;
    private final PreparedStatement deleteContainer;
    private final PreparedStatement insertSlot;
    private final PreparedStatement find;
    private final PreparedStatement prune;

    public SqliteStashRepository(Path databaseFile) {
        this(url(databaseFile), true);
    }

    public SqliteStashRepository(String jdbcUrl) {
        this(jdbcUrl, false);
    }

    private SqliteStashRepository(String jdbcUrl, boolean createParent) {
        try {
            if (createParent) {
                Path file = pathFromUrl(jdbcUrl);
                if (file.getParent() != null) {
                    Files.createDirectories(file.getParent());
                }
            }
            connection = DriverManager.getConnection(jdbcUrl);
            connection.setAutoCommit(true);
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
                statement.execute("PRAGMA busy_timeout = " + BUSY_TIMEOUT_MS);
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS container (
                            location_key TEXT PRIMARY KEY,
                            dimension_id TEXT NOT NULL,
                            x INTEGER NOT NULL,
                            y INTEGER NOT NULL,
                            z INTEGER NOT NULL,
                            kind TEXT NOT NULL,
                            label TEXT NOT NULL,
                            seen_at INTEGER NOT NULL
                        )
                        """);
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS slot (
                            location_key TEXT NOT NULL,
                            slot_index INTEGER NOT NULL,
                            item_id TEXT NOT NULL,
                            count INTEGER NOT NULL,
                            display_name TEXT NOT NULL,
                            search_name TEXT NOT NULL,
                            PRIMARY KEY (location_key, slot_index),
                            FOREIGN KEY (location_key) REFERENCES container(location_key) ON DELETE CASCADE
                        )
                        """);
                statement.execute("CREATE INDEX IF NOT EXISTS idx_slot_search ON slot(search_name)");
                statement.execute("CREATE INDEX IF NOT EXISTS idx_slot_item ON slot(item_id)");
            }
            replaceContainer = connection.prepareStatement(REPLACE_CONTAINER);
            deleteSlots = connection.prepareStatement(DELETE_SLOTS);
            deleteContainer = connection.prepareStatement(DELETE_CONTAINER);
            insertSlot = connection.prepareStatement(INSERT_SLOT);
            find = connection.prepareStatement(FIND);
            prune = connection.prepareStatement(PRUNE);
        } catch (SQLException | IOException exception) {
            throw new StashStorageException("stash.sqlite не открылся", exception);
        }
    }

    @Override
    public synchronized void replace(ContainerSnapshot snapshot) {
        try {
            connection.setAutoCommit(false);
            if (snapshot.slots().isEmpty()) {
                deleteContainer.setString(1, snapshot.locationKey());
                deleteContainer.executeUpdate();
            } else {
                bindContainer(snapshot);
                replaceContainer.executeUpdate();
                deleteSlots.setString(1, snapshot.locationKey());
                deleteSlots.executeUpdate();
                for (StoredSlot slot : snapshot.slots()) {
                    insertSlot.setString(1, snapshot.locationKey());
                    insertSlot.setInt(2, slot.slotIndex());
                    insertSlot.setString(3, slot.itemId());
                    insertSlot.setInt(4, slot.count());
                    insertSlot.setString(5, slot.displayName());
                    insertSlot.setString(6, slot.searchName());
                    insertSlot.addBatch();
                }
                insertSlot.executeBatch();
            }
            connection.commit();
        } catch (SQLException exception) {
            rollback();
            throw new StashStorageException("сундук не записался", exception);
        } finally {
            enableAutoCommit();
        }
    }

    @Override
    public synchronized void forget(String locationKey) {
        try {
            deleteContainer.setString(1, locationKey);
            deleteContainer.executeUpdate();
        } catch (SQLException exception) {
            throw new StashStorageException("запись не удалилась", exception);
        }
    }

    @Override
    public synchronized List<SearchHit> find(String displayPattern, String itemPattern, int limit) {
        try {
            find.setString(1, displayPattern);
            find.setString(2, itemPattern);
            find.setInt(3, limit);
            try (ResultSet rows = find.executeQuery()) {
                List<SearchHit> hits = new ArrayList<>();
                while (rows.next()) {
                    hits.add(new SearchHit(
                            rows.getString("location_key"),
                            rows.getString("dimension_id"),
                            rows.getInt("x"),
                            rows.getInt("y"),
                            rows.getInt("z"),
                            ContainerKind.fromStored(rows.getString("kind")),
                            rows.getString("label"),
                            rows.getString("item_id"),
                            rows.getString("display_name"),
                            rows.getInt("total")
                    ));
                }
                return List.copyOf(hits);
            }
        } catch (SQLException exception) {
            throw new StashStorageException("поиск не прошёл", exception);
        }
    }

    @Override
    public synchronized void pruneOldest(int maxContainers) {
        try {
            prune.setInt(1, maxContainers);
            prune.executeUpdate();
        } catch (SQLException exception) {
            throw new StashStorageException("старые сундуки не вычистились", exception);
        }
    }

    @Override
    public synchronized void close() {
        closeQuietly(prune);
        closeQuietly(find);
        closeQuietly(insertSlot);
        closeQuietly(deleteContainer);
        closeQuietly(deleteSlots);
        closeQuietly(replaceContainer);
        try {
            connection.close();
        } catch (SQLException exception) {
            throw new StashStorageException("stash.sqlite не закрылся", exception);
        }
    }

    private void bindContainer(ContainerSnapshot snapshot) throws SQLException {
        replaceContainer.setString(1, snapshot.locationKey());
        replaceContainer.setString(2, snapshot.dimensionId());
        replaceContainer.setInt(3, snapshot.x());
        replaceContainer.setInt(4, snapshot.y());
        replaceContainer.setInt(5, snapshot.z());
        replaceContainer.setString(6, snapshot.kind().name());
        replaceContainer.setString(7, snapshot.label());
        replaceContainer.setLong(8, snapshot.seenAtMillis());
    }

    private void rollback() {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // исходная ошибка важнее, откат уже не спасти
        }
    }

    private void enableAutoCommit() {
        try {
            connection.setAutoCommit(true);
        } catch (SQLException exception) {
            throw new StashStorageException("транзакция так и не закрылась", exception);
        }
    }

    private static void closeQuietly(PreparedStatement statement) {
        try {
            statement.close();
        } catch (SQLException ignored) {
            // close() запроса не должен затереть ошибку соединения
        }
    }

    private static String url(Path databaseFile) {
        return "jdbc:sqlite:" + databaseFile.toAbsolutePath();
    }

    private static Path pathFromUrl(String jdbcUrl) {
        String prefix = "jdbc:sqlite:";
        if (!jdbcUrl.startsWith(prefix)) {
            throw new StashStorageException("кривой адрес базы");
        }
        return Path.of(jdbcUrl.substring(prefix.length()));
    }
}
